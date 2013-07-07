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

	(loop [crawlQueue [{:url starturl, :depth 0}] currentDepth 0 crawldata {}]		

		(if (> (:depth (first crawlQueue)) depth)
			;if maximum depth is reached, stop crawling process
			crawldata

			;check if url already has been crawled
			(if (contains? crawldata (:url (first crawlQueue)))
				;continue loop with next url
				(recur (rest crawlQueue) (inc (:depth (first crawlQueue))) crawldata)

				;else parse the url
				(let [httpResponse (http-kit/get (:url (first crawlQueue))) 
						siteDataSet (parser/doParse (:body @httpResponse) currentDepth)]

					(do
						;print data set if wanted
						(if (true? printResults) (printReport (:url (first crawlQueue)) siteDataSet))
								
						;continue working through crawl queue
						(recur 
							(rest (into crawlQueue (map #(sorted-map :url % :depth (inc currentDepth)) (:links siteDataSet)))) 
							(inc (:depth (first crawlQueue))) 
							(assoc crawldata (:url (first crawlQueue)) siteDataSet)
						)
					)
				)
			)
		)
	)
)

(defn -main [& args] ;first argument has to be the start url, second the search depth

	(crawl (nth args 0) (Integer/parseInt (nth args 1)) true)

	nil
)