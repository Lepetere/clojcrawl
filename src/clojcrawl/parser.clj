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
	"takes a string containing an html tag and checks if it's an anchor tag"
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
	[crawldataKeywordMap newFoundKeywords]

		(loop [keywordMap crawldataKeywordMap keywordVector newFoundKeywords]
			(if (empty? keywordVector)
				keywordMap
				;keywordVector not empty, insert next keyword into map:
				(recur
					(let [keywordCount (get keywordMap (first keywordVector))]
						(assoc keywordMap (first keywordVector) (if (nil? keywordCount) 1 (inc keywordCount))) ;(assoc map key val)
					)
					(rest keywordVector)
				)
			)
		)
)

(defn doParse 

	"This method separates tags and text between tags; call this method to start the parsing process"

	[datastring depth]

	(loop [position 0 keywords {} links #{}]

		;end loop if there no more tag beginnings
		(if (not= (.indexOf datastring "<" position) -1)
			(let [
				;find next tag beginning
				nextposition (.indexOf datastring "<" position)

				;find the end of the tag
				oneFurtherPosition (.indexOf datastring ">" nextposition)]

							(recur
								;increase the position of the last tag's end by one
								(inc oneFurtherPosition)
								;add the next keywords, if there is space between the last and the next tag, otherwise just pass keyword map
								(if (< position (dec nextposition)) 
									(collectAndInsertKeywords keywords (processFreeText (subs datastring position nextposition))) 
									keywords
								)
								;add a potentially found link address to the link set
								(conj links (processTag (subs datastring (inc nextposition) oneFurtherPosition)))
							)
						)

			;quit loop and return hash map with the crawl data
			(sorted-map :depth depth, :keywords keywords, :links (filter #(not (or (= % "") (nil? %))) links))
		)
	)
)