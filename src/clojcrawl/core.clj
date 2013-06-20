(ns clojcrawl.core
	(:require [org.httpkit.client :as http-kit])
	(:require [clojcrawl.parser :as parser])
)

(defn crawl [starturl depth]
	;(def theresultvecmap vector)

	(let [response1 (http-kit/get starturl)]
		;; Other keys :headers :body :error :opts
		(println "ergebnis:   " (:keywords (parser/doParse (:body @response1) starturl)))
		;; returned data structure: {:url starturl :links [] :keywords []}
	)
)

(defn -main [& args] ;first argument has to be the start url, second the search depth

	(crawl (nth args 0) (nth args 1))
)