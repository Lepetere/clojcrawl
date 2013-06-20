(ns clojcrawl.parser)

(def stop_words [
	;german
	"der", "die", "das", "einer", "eine", "ein", "und", "oder", "doch", "an", "in", "nicht", 
	;english
	"a", "I", "of", "so", "this", "the", "it", "you", "and", "can", "can't", "cause", "any", "got", "have", "is", "as",
	;character
	".", ",", ";", ":", "?", "!", "(", ")",
	;html entities
	"nbsp"])

(defn getLinkAddress
	"searches an anchor tag for a href attribute and returns its link address"
	[datastring]
	;only proceed if a tag contains "href"
	(if (.contains datastring "href")
		(let [urlposition (+ (.indexOf datastring "href=") 6) 
				findurlend (.indexOf datastring " " urlposition) 
					urlend (if (not (< findurlend 0)) (dec findurlend) (- (count datastring) 1))]

						(subs datastring urlposition urlend)
		)
	)
)

(defn processTag
	[datastring]
	;filter out empty strings, html comments and closing tags
	(if (and (not (clojure.string/blank? datastring))  (and (not= (first datastring) \!) (not= (first datastring) \/)))
		;only proceed if is an anchor tag
		(if (= (first datastring) \a)
			(let [linkAddress (getLinkAddress datastring)]
				(if (not= (first linkAddress) \#)
					(str linkAddress)
				)
			)
		)
	)
)

(defn separateKeywords
	"returns a vector of words found in the string, all lower case"
	[datastring]

	(map #(clojure.string/lower-case %) (filter #(not (clojure.string/blank? %)) (clojure.string/split datastring #"\s|\p{Punct}")))
)

(defn throwOutStopwords
	"returns a vector of keywords found in the string with alls stop words sorted out"
	[datastring]

	(filter 
		(fn [k] (not (first (filter #(= k %) stop_words))))
			(separateKeywords datastring)
	)
)

(defn processFreeText 
	"if string is not empty this method calls further methods to collect keywords"
	[datastring]

	(if (not (clojure.string/blank? datastring))
			(throwOutStopwords (clojure.string/trim-newline datastring))
	)
)

(defn collectAndInsertKeywords
	"This method counts the occurrences of all keywords and adds them to the given keyword map"
	[datastring]
	(comment (into (:keywords crawldata) keywords))
)

(defn doParse [datastring url]

	(loop [position 0 keywords [] links #{}]

		;end loop if there no more tag beginnings
		(if (not= (.indexOf datastring "<" position) -1)
			(let [
				;find next tag beginning
				nextposition (.indexOf datastring "<" position)
				;get the next keywords, if there is space between the last and the next tag, otherwise pass nil
				keywordsvec (if (< position (dec nextposition)) (processFreeText (subs datastring position nextposition)) nil)
				;find the end of the tag
				oneFurtherPosition (.indexOf datastring ">" nextposition) 
				link (processTag (subs datastring (inc nextposition) oneFurtherPosition))]

							(recur (inc oneFurtherPosition) (into keywords keywordsvec) (conj links link))
						)

			(sorted-map :url url, :keywords keywords, :links links)
		)
	)
)