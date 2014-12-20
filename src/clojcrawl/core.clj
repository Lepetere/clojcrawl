(ns clojcrawl.core
  (:require [org.httpkit.client :as http-kit])
  (:require [clojcrawl.parser :as parser]
            [clojcrawl.data-sink :as data-sink]))

(def ^:dynamic number-of-concurrent-requests 10)
(def ^:dynamic maximum-crawl-depth)

(declare running-requests-watch)

;; running-requests contains the futures of running Http GETs with the crawled url as key
;; can be seen as a thread pool
(def running-requests (add-watch (atom {}) :requests-watch running-requests-watch))
;; contains the already crawled urls to a certain size
(def crawled-urls (atom #{}))
;; a queue containing the urls found in the crawling process
;; the queue contains 'raw' urls as strings with the crawl depth associated as meta data
(def urls-to-be-crawled (agent (clojure.lang.PersistentQueue/EMPTY)))

(defn is-url-already-crawled?
  "Checks if a url has already been crawled by checking the crawled urls atom.
  Since the atom only holds a certain amount of urls in-memory, this will only
  return true if the crawl has been 'recent'."
  ;; later this method could additionally make a db query to check if a url already has been crawled
  [url]
  (contains? @crawled-urls url))

(defn maximum-depth-reached?
  "Checks if the first element of the crawl queue has reached the maximum crawl depth."
  [url-queue]
  (> (:depth (meta (first url-queue))) maximum-crawl-depth))

(defn register-crawled-url
  "Adds a url that has already been crawled to the crawled-urls atom,
  so it will not be crawled again."
  ;; TO DO: keep an eye on the size of the set; could also happen via a watch on the atom
  [url]
  (swap! crawled-urls conj url))

(defn add-links-to-crawl-queue
  "Adds an unordered set of links to the urls-to-be-crawled agent.
  Sets the :depth meta data of the links according to the depth passed as second argument."
  [url-queue links depth]
  (into url-queue (map #(with-meta % {:depth depth}) links)))

(defn fire-request
  "Launches the GET of a url in a new future that is then stored in the running-requests atom.
  The future is responsible to remove itself from the atom once the GET is done, so that the
  watch on the atom can launch the next GET (take-url). When the future is realized, it will
  trigger the parsing of the retreived data."
  [url]
  (swap! running-requests assoc url
    (future 
      (let [crawldata (parser/do-parse (:body @(http-kit/get url)) (:depth (meta url)))]
        (send urls-to-be-crawled add-links-to-crawl-queue (:links crawldata) (:depth (meta url)))
        (data-sink/print-report crawldata)
        ;; then remove this future from the running-requests atom
        (swap! running-requests dissoc url)))))

(defn remove-future-from-running-requests
  "Removes a future from the running-requests atom. This method will be called by the future itself
  in order to remove itself from running-requests.
  It takes as argument the url that the future is GETting and that at the same time is the key 
  under which the future is stored in the running-requests atom."
  [url]
  (swap! running-requests dissoc url))

(defn take-url
  "Launches a crawl of the next url on the crawl queue. The method makes sure to only crawl urls which 
  haven't been crawled yet and stops the crawling process if there are no more urls in the crawl-queue.

  Takes as an argument the url queue, so it can be applied to the agent containing the queue."
  [url-queue]
  (loop [url-queue url-queue]
    (if (< (count url-queue) 1)
      (throw (Exception. "No more urls to crawl in the url queue.")) ; TO DO: start a new thread, wait some time, then try again, if still empty throw an error
      (if (is-url-already-crawled? (first url-queue))
        (recur (pop url-queue))
        (if (maximum-depth-reached? url-queue)
          (throw (Exception. "Maximum crawl depth reached."))
          (do
            (fire-request (first url-queue))
            (pop url-queue)))))))

(defn launch-next-crawl
  "Starts a crawl of the next url."
  []
  (send-off urls-to-be-crawled take-url))

;; this watch observes the size of the running-requests atom and launches new requests
(defn running-requests-watch
  [key identity old new]
  (println "Concurrent requests: " (count new))
  (if (< (count new) number-of-concurrent-requests)
    (launch-next-crawl)))

(defn launch-crawl
  "Launches the crawler."
  [starturl maximum-depth number-of-concurrent-requests]
  (fire-request (with-meta starturl {:depth 0})))

(defn crawl 
  [starturl depth number-of-concurrent-requests print-results]
  (println "\nnumber of concurrent requests: " number-of-concurrent-requests "\n")
  (loop [crawlQueue (-> (clojure.lang.PersistentQueue/EMPTY) (conj {:url starturl, :depth 0})) 
		 crawldata {:amountOfUrlsCrawled 0}]		
    (if (or (empty? crawlQueue) (> (:depth (first crawlQueue)) depth))
      ;;if maximum depth is reached or queue is empty, stop crawling process
      crawldata
      ;;check if url already has been crawled
      (if (contains? crawldata (:url (first crawlQueue)))
        ;;continue loop with next url
        (recur (pop crawlQueue) crawldata)
        ;;else parse the url
        (let [httpResponse (http-kit/get (:url (first crawlQueue))) 
			  siteDataSet (parser/do-parse (:body @httpResponse) (:depth (first crawlQueue)))]
          (do
            ;;print data set if wanted
            (if (true? print-results) (data-sink/print-report (:url (first crawlQueue)) siteDataSet))
            ;;continue working through crawl queue
            (recur
            ;;update crawlqueue -> pop element, add new found links (as maps with the right depth value)
            (pop (into crawlQueue (map #(sorted-map :url % :depth (inc (:depth (first crawlQueue)))) (:links siteDataSet))))
            ;;put new site data set in crawldata map, increase the amount of crawled sites
            (-> (assoc crawldata (:url (first crawlQueue)) siteDataSet) (assoc :amountOfUrlsCrawled (inc (:amountOfUrlsCrawled crawldata)))))))))))

(defn -main 
  "Call like this: lein run 'http://www.peterfessel.com' 5 20

  First argument has to be the start url, second the search depth.

  The third argument is the number of requests that the crawler should run concurrently. It is optional and will default to 10 if omitted."
  [& args]
  (println "Total URLs crawled:  " 
    (:amountOfUrlsCrawled (crawl 
      (nth args 0) 
      (Integer/parseInt (nth args 1))
      (if (<= (count args) 2) number-of-concurrent-requests (Integer/parseInt (nth args 2)))
      true)))
  #_(binding [maximum-crawl-depth (Integer/parseInt (nth args 1))
      number-of-concurrent-requests (if (<= (count args) 2) number-of-concurrent-requests (Integer/parseInt (nth args 2)))]
      (launch-crawl (nth args 0) maximum-crawl-depth number-of-concurrent-requests)))
