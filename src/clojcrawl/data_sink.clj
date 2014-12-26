(ns clojcrawl.data-sink)

(defn print-report
  "prints out a report of a site data set"
  [url crawldata]
  (println "url:              " url)
  (println "depth:            " (pr-str (:depth crawldata)))
  (println "keywords found:   " (pr-str (:keywords crawldata)))
  (println "links found:      " (pr-str (:links crawldata)))
  (println))

(defn print-short-report
  "prints out a short report of a site data set"
  [url crawldata]
  (println "url" url "crawled;" (count (:links crawldata)) "links found ( crawl depth" (:depth crawldata) ")"))