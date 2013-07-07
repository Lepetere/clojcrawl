This is a small webcrawler written in Clojure.

Use like this:
Call main method using Leiningen with the URL where you wanna start crawling as first argument and the depth you want to crawl as second argument.
For example 'lein run "https://github.com/Lepetere/clojcrawl" 3'

While running the program will print out a report on the crawled sites on the command line including the links and the keywords found on each site.