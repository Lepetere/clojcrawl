(ns clojcrawl.data-sink)

(defn print-report
  "prints out a report of a site data set"
  [url crawldata]
  (println "url:              " url)
  (println "depth:            " (pr-str (:depth crawldata)))
  (println "keywords found:   " (pr-str (:keywords crawldata)))
  (println "links found:      " (pr-str (:links crawldata)))
  (println))