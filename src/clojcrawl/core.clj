(ns clojcrawl.core
	(:require [org.httpkit.client :as http-kit])
	(:require [clojcrawl.parser :as parser])
)

(defn printReport
	"prints out a report of a site data set"
	[url crawldata]

	(println "url:              " url)
	(println "depth:            " (pr-str (:depth crawldata)))
	(println "keywords found:   " (pr-str (:keywords crawldata)))
	(println "links found:      " (pr-str (:links crawldata)))
	(println)
)

(defn crawl [starturl depth printResults]

	(loop [crawlQueue (-> (clojure.lang.PersistentQueue/EMPTY) (conj {:url starturl, :depth 0})) 
			crawldata {:amountOfUrlsCrawled 0}]		

		(if (or (empty? crawlQueue) (> (:depth (first crawlQueue)) depth))
			;if maximum depth is reached or queue is empty, stop crawling process
			crawldata

			;check if url already has been crawled
			(if (contains? crawldata (:url (first crawlQueue)))
				;continue loop with next url
				(recur (pop crawlQueue) crawldata)

				;else parse the url
				(let [httpResponse (http-kit/get (:url (first crawlQueue))) 
						siteDataSet (parser/doParse (:body @httpResponse) (:depth (first crawlQueue)))]

					(do
						;print data set if wanted
						(if (true? printResults) (printReport (:url (first crawlQueue)) siteDataSet))
								
						;continue working through crawl queue
						(recur
							;update crawlqueue -> pop element, add new found links (as maps with the right depth value)
							(pop (into crawlQueue (map #(sorted-map :url % :depth (inc (:depth (first crawlQueue)))) (:links siteDataSet))))
							;put new site data set in crawldata map, increase the amount of crawled sites
							(-> (assoc crawldata (:url (first crawlQueue)) siteDataSet) (assoc :amountOfUrlsCrawled (inc (:amountOfUrlsCrawled crawldata))))
						)
					)
				)
			)
		)
	)
)

(defn -main [& args] ;first argument has to be the start url, second the search depth

	(println "Total URLs crawled:  " (:amountOfUrlsCrawled (crawl (nth args 0) (Integer/parseInt (nth args 1)) true)))

)