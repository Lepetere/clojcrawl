(ns clojcrawl.core
  (:require [org.httpkit.client :as http-kit])
  (:require [clojcrawl.parser :as parser]
            [clojcrawl.data-sink :as data-sink]))

(def ^:const default-number-of-concurrent-requests 10)

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
  "Call like this: lein run 'http://www.peterfessel.com' 2 5

  First argument has to be the start url, second the search depth.

  The third argument is the number of requests that the crawler should run concurrently. It is optional and will default to 10 if omitted."
  [& args]
  (println "Total URLs crawled:  " 
    (:amountOfUrlsCrawled (crawl 
      (nth args 0) 
      (Integer/parseInt (nth args 1))
      (if (<= (count args) 2) default-number-of-concurrent-requests (Integer/parseInt (nth args 2)))
      true))))