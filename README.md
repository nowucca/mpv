mpv
===

Goal:
Rank HBO GO available movies by popularity based on matching Wikipedia page views

Method:
1. Fetch the list of HBO movies from:  http://xfinitytv.comcast.net/movie.widget
2. For each movie title name look for Wikipedia page views using the end-point http://stats.grok.se/json/en/<http://stats.grok.se/json/en/201402/Behind%20the%20Candelabra><YYYYMM>/<title name> (e.g.: the Feb 2014 stats for the movie Behind the Candelabra  http://stats.grok.se/json/en/201402/Behind%20the%20Candelabra)
3. Output a list of title names sorted by sum of the total page views during the last month

Bonus: 
1. Optimize for best performance. 
2. Think of ways to improve movie title to Wikipeida page match. 
3. Output results as JSON


process
=======
* determined source of movies was xhtml, chose JDOM2 library to parse it.
* determined how to model a movie by seeing which attributes were mandatory or not.
                List<String> attributeNames = Arrays.asList("id", "data-rl", "data-d", "data-p", "data-n", "data-ln", "data-t", "isAlpha", "data-v",
                    "data-type", "data-g", "data-pop", "data-alpha", "href");
                boolean isOptional = false;
                for (Element e: moviesElements) {
                    if (e.getAttribute(attributeName) == null) {
                        isOptional = true;
                        System.out.println("Attribute \"" + attributeName + "\" is optional.");
                        break;
                    }
                }
                if ( !isOptional ) {
                    System.out.println("Attribute \"" + attributeName + "\" is mandatory.");
                }
* chose rxJava library for API to MovieProvider to allow for different implementations other than synchronous network JDOM later

thoughts
========

* decided due to time constraints to use an expensive non-pipelining HttpURLConnection and read JSON for the page view data.

* one idea to improve handling and parallelism in MovieObserver would be to submit page view calculations to an executor which could ideally use pipelined HTTP 1.1.  this would avoid connection setup and round-trip time latency.

* should write unit tests and complete modelling of the Movie object

* to improve title matching we could use stemming, normalizing and matching with a lucene index from queries to wikipedia title, built from the larger datasets downloadable from stats.grok.se.
  specifically:  HBO Go title could be normalizing, used to search a lucene index to find possible wikipedia title.  we could sue release year if able to disambiguate.
