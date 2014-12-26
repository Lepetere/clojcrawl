(ns clojcrawl.parser)

(def stop_words [
  ;german
  "der" "die" "das" "einer" "eine" "ein" "und" "oder" "doch" "an" "in" "nicht" 
  ;english
  "a" "I" "of" "so" "this" "the" "it" "you" "and" "can" "can't" "cause" "any" "got" "have" "is" "as"
  ;character
  "." "" ";" ":" "?" "!" "(" ")"
  ;html entities
  "nbsp"])

(defn get-link-address
  "searches an anchor tag for a href attribute and returns its link address"
  [datastring]
  ;only proceed if a tag contains "href"
  (if (.contains datastring "href")
    (let [urlposition (+ (.indexOf datastring "href=") 6) 
          findurlend (.indexOf datastring " " (+ urlposition 3)) ;plus 3 is a workaround because href content can start with whitespace
          urlend (if (not (or (< findurlend 0) (> findurlend (count datastring)))) (dec findurlend) (- (count datastring) 1))]

            (if (not (< (- urlend urlposition) 0)) (subs datastring urlposition urlend) nil))))

(defn process-tag
  "takes a string containing an html tag and checks if it's an anchor tag"
  [datastring]
  ;filter out empty strings, html comments and closing tags
  (if (and (not (clojure.string/blank? datastring))  (and (not= (first datastring) \!) (not= (first datastring) \/)))
    ;only proceed if is an anchor tag
    (if (= (first datastring) \a)
      (let [linkAddress (get-link-address datastring)]
        (if (not= (first linkAddress) \#)
          (str linkAddress))))))

(defn separate-keywords
  "returns a vector of words found in the string, all lower case"
  [datastring]
  (map #(clojure.string/lower-case %) (filter #(not (clojure.string/blank? %)) (clojure.string/split datastring #"\s|\p{Punct}"))))

(defn throw-out-stopwords
  "returns a vector of keywords found in the string with alls stop words sorted out"
  [datastring]
  (filter 
    (fn [k] (not (first (filter #(= k %) stop_words))))
      (separate-keywords datastring)))

(defn process-free-text 
  "if string is not empty this method calls further methods to collect keywords"
  [datastring]
  (if (not (clojure.string/blank? datastring))
    (throw-out-stopwords (clojure.string/trim-newline datastring))))

(defn collect-and-insert-keywords
  "This method counts the occurrences of all keywords and adds them to the given keyword map"
  [crawldataKeywordMap newFoundKeywords]
    (loop [keywordMap crawldataKeywordMap keywordVector newFoundKeywords]
      (if (empty? keywordVector)
        keywordMap
        ;keywordVector not empty, insert next keyword into map:
        (recur
          (let [keywordCount (get keywordMap (first keywordVector))]
          	;(assoc map key val)
            (assoc keywordMap (first keywordVector) (if (nil? keywordCount) 1 (inc keywordCount))))
            (rest keywordVector)))))

(defn do-parse 
  "This method separates tags and text between tags; call this method to start the parsing process"
  [datastring depth]
  (loop [position 0 keywords {} links #{}]
    ;end loop if there no more tag beginnings
      (if (and (not (nil? datastring)) (not= (.indexOf datastring "<" position) -1))
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
                (collect-and-insert-keywords keywords (process-free-text (subs datastring position nextposition)))
                keywords)
                ;add a potentially found link address to the link set
                  (conj links (process-tag (subs datastring (inc nextposition) oneFurtherPosition)))))
        ;quit loop and return hash map with the crawl data
        (sorted-map :depth depth, :keywords keywords, :links (filter #(and (not (or (= % "") (nil? %) (.endsWith % ".pdf"))) (.startsWith % "http:")) links)))))
