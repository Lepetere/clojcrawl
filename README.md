This is a small webcrawler written in Clojure.

## Use like this:
Call main method using Leiningen with the URL where you want to start crawling as first argument and the depth you want to crawl as second argument. Pass the maximum of concurrent requests as optional third argument (will otherwise default to 10).


For example 

```
lein run "http://www.peterfessel.com" 5 20
```

---

I am using this project as a playground to learn Clojure better.

####Features so far:
- concurrent crawling
- self written simple parser, which filters out links and keywords from a website

####Features to be added:
- database connection
- optimizing the parser
- implement machine learning for the parser (to identify stopwords and rate the importance of keywords)