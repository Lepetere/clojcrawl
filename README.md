This is a small webcrawler written in Clojure.

## Use like this:
Call main method using Leiningen with the URL where you wanna start crawling as first argument and the depth you want to crawl as second argument.


For example 

```
lein run "http://www.google.de" 2
```

Optionally you can pass the maximum number of concurrent requests as a third argument. If omitted, it will default to 10.

While running the program will print out a report on the crawled sites on the command line including the links and the keywords found on each site. From the keywords, the crawler will filter out certain english and german stop words, given in the parser.clj file.