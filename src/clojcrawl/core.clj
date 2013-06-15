(ns clojcrawl.core
	(:require [org.httpkit.client :as http-kit])
	(:require [clojcrawl.parser :as parser])
)

(defn -main [& args]
  (let [response1 (http-kit/get (nth args 0))]
		;; Other keys :headers :body :error :opts
		(println (parser/doParse (:body @response1)))
	)
)
