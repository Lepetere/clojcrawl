(ns clojcrawl.core
  (:require [org.httpkit.client :as http-kit])
  (:require [clojcrawl.parser :as parser]
            [clojcrawl.data-sink :as data-sink]))

(def ^:dynamic number-of-concurrent-requests 10)
(def ^:dynamic maximum-crawl-depth)

(declare launch-next-crawl)

;; contains the already crawled urls to a certain size
(def crawled-urls (atom #{}))
;; a queue containing the urls found in the crawling process
;; the queue contains 'raw' urls as strings with the crawl depth associated as meta data
(def urls-to-be-crawled (agent (clojure.lang.PersistentQueue/EMPTY)))

;; running-requests contains the futures of running Http GETs with the crawled url as key
;; can be seen as a thread pool
(def running-requests (add-watch (atom {}) :requests-watch 
  ;; observe the size of running-requests and launch new requests
  (fn [key identity old new]
    (println "Concurrent requests: " (count new))
    (println identity)
    (println "queue length: " (count @urls-to-be-crawled))
    ;; automatically launch next crawl if the current number of requests is smaller than the maximum number of current requests
    (if (and (< (count new) number-of-concurrent-requests) (> (count @urls-to-be-crawled) 0))
      ;; if there is no content (yet) in the queue, wait for all agent actions to finish
      (if (< (count @urls-to-be-crawled) 1)
        (do
          (println "await urls-to-be-crawled")
          (await urls-to-be-crawled)
          (if (< (count @urls-to-be-crawled) 1)
            (launch-next-crawl)))
        (launch-next-crawl))))))

(defn is-url-already-crawled?
  "Checks if a url has already been crawled by checking the crawled urls atom.
  Since the atom only holds a certain amount of urls in-memory, this will only
  return true if the crawl has been 'recent'."
  ;; later this method could additionally make a db query to check if a url already has been crawled
  [url]
  (contains? @crawled-urls (:url url)))

(defn maximum-depth-reached?
  "Checks if the first element of the crawl queue has reached the maximum crawl depth."
  [url-queue]
  (> (:depth (first url-queue)) maximum-crawl-depth))

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
  (into url-queue (map #(hash-map :url % :depth depth) links)))

(defn fire-request
  "Launches the GET of a url in a new future that is then stored in the running-requests atom.
  The future is responsible to remove itself from the atom once the GET is done, so that the
  watch on the atom can launch the next GET (take-url). When the future is realized, it will
  trigger the parsing of the retreived data."
  [url]
  (let [url-string (:url url)
        depth (:depth url)]
    (register-crawled-url url-string)
    (swap! running-requests assoc url-string
      (future 
        (let [crawldata (parser/do-parse (:body @(http-kit/get url-string)) depth)]
          (send urls-to-be-crawled add-links-to-crawl-queue (:links crawldata) depth)
          (data-sink/print-report url-string crawldata))
          ;; then remove this future from the running-requests atom
          (swap! running-requests dissoc url-string)))))

(defn take-url
  "Launches a crawl of the next url on the crawl queue. The method makes sure to only crawl urls which 
  haven't been crawled yet and stops the crawling process if there are no more urls in the crawl-queue.

  Takes as an argument the url queue, so it can be applied to the agent containing the queue."
  [url-queue]
  ;; run a loop over the url-queue until the next url that hasn't been crawled yet is found
  (loop [queue url-queue]
    (if (< (count queue) 1)
      (throw (Exception. "No more urls to crawl in the url queue."))
        (if (not (is-url-already-crawled? (first queue)))
          (if (maximum-depth-reached? queue)
            (throw (Exception. "Maximum crawl depth reached."))
            (do
              (fire-request (first queue))
              (pop queue)))
          (recur (pop queue))))))

(defn launch-next-crawl
  "Starts a crawl of the next url."
  []
  (send-off urls-to-be-crawled take-url))

(defn launch-crawl
  "Launches the crawler."
  [starturl maximum-depth number-of-concurrent-requests]
  (fire-request {:url starturl :depth 0}))

(defn -main 
  "Call like this: lein run 'http://www.peterfessel.com' 5 20

  First argument has to be the start url, second the search depth.

  The third argument is the number of requests that the crawler should run concurrently. It is optional and will default to 10 if omitted."
  [& args]
  (try
    (binding [maximum-crawl-depth (Integer/parseInt (nth args 1))
      number-of-concurrent-requests (if (<= (count args) 2) number-of-concurrent-requests (Integer/parseInt (nth args 2)))]
      (launch-crawl (nth args 0) maximum-crawl-depth number-of-concurrent-requests))
    (catch Exception e 
      (do
        (println (agent-error urls-to-be-crawled))
        (shutdown-agents)
        (println "\nCrawler shut down. Reason: " (.getMessage e))
        (println "\nStack trace: " (.printStackTrace e))))))
