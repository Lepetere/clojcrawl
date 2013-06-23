(ns clojcrawl.core
	(:require [org.httpkit.client :as http-kit])
	(:require [clojcrawl.parser :as parser])
)

(defn printReport
	"prints out a report after the crawl procedure"
	[crawldata]

	(println "url crawled:   " (pr-str (:url crawldata)))
	(println "links found:   " (pr-str (:links crawldata)))
	(println "keywords found:   " (pr-str (:keywords crawldata)))
	(println "urls crawled:   " "?")
)

(defn crawl [starturl depth]

	(comment (loop [crawlQueue starturl currentDepth 0 crawldata {}]

		;check if url already has been crawled
		(if (contains? crawldata (first crawlQueue))
			;continue loop with next url
			(recur (rest crawlQueue) (inc (:depth (first crawlQueue))) crawldata)
			;parse the url
			(let [httpResponse (http-kit/get (first crawlQueue)) 
					siteDataSet (parser/doParse (:body @httpResponse) (comment currentDepth))] ;TO DO: statt starturl sollte depth übergeben werden

					(if (= (:depth siteDataSet) depth)
						;stop crawling process
						(assoc crawldata (first crawlQueue) siteDataSet)
						;else continue working through crawl queue
						(recur (rest (into crawlQueue (:links siteDataSet))) (inc (:depth (first crawlQueue))) (assoc crawldata (first crawlQueue) siteDataSet))
					)
			)
		)
	))


	(let [response1 (http-kit/get starturl)]
		;; Other keys :headers :body :error :opts
		(printReport (parser/doParse (:body @response1) starturl))
		;; returned data structure: {:url starturl :links [] :keywords []}
	)
)

(defn -main [& args] ;first argument has to be the start url, second the search depth

	(crawl (nth args 0) (nth args 1))
)