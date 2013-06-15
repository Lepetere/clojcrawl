(ns clojcrawl.parser)

(defn processLink [data]
	;only proceed if a tag contains "href"
	(if (.contains data "href")
		(let [urlposition (+ (.indexOf data "href=") 6) 
				findurlend (.indexOf data " " urlposition) 
					urlend (if (not (< findurlend 0)) (dec findurlend) (- (count data) 1))]

					(println "    link:   " (subs data urlposition urlend))
		)
	)
)

(defn processTag [data]
	;filter out empty strings, html comments and closing tags
	(if (and (not (clojure.string/blank? data))  (and (not= (first data) \!) (not= (first data) \/)))
		(if (= (first data) \a) 
			(processLink data)
		)
	)
)

(defn processFreeText [data] 
	(if (not (clojure.string/blank? data))
		(println "free text:   " (clojure.string/trim-newline data))
	)
)

(defn doParse [datastring]
	
	(loop [position 0] 
		(let [nextposition (.indexOf datastring "<" position)]
			(if (not= nextposition -1)
				(do
					(if (< position (dec nextposition))
						(processFreeText (subs datastring position nextposition))
					)
					(let [oneFurtherPosition (.indexOf datastring ">" nextposition)]
						(do
							(processTag (subs datastring (inc nextposition) oneFurtherPosition))
							(recur (inc oneFurtherPosition))
						)
					)
				)
			)
		)
	)
)