package com.nowucca.mpv.core;

import com.nowucca.mpv.util.UTF8;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import rx.Observable;
import rx.Observer;

/**
 * Rank HBO GO available movies by popularity based on matching Wikipedia page views.
 *
 * Depends upon network access to these URLs:
 * <ul>
 *     <li>{@link #HBO_GO_MOVIE_LIST_URL}</li>
 *     <li>{@link #WIKIPEDIA_PAGE_URL_PREFIX}</li>
 * </ul>
 */
public class MoviePageViews {

    // External data sources
    public static final String HBO_GO_MOVIE_LIST_URL = "http://xfinitytv.comcast.net/movie.widget";
    public static final String WIKIPEDIA_PAGE_URL_PREFIX = "http://stats.grok.se/json/en/201408/";

    /**
     * Get a list of movies and rank them by page view using JSON output.
     */
    public static void main(String[] args) throws Exception {
        Observable<Movie> movies = getMovies();
        movies.take(50).subscribe(new MovieObserver());
    }


    /**
     * Provides a list of Movies to be ranked.
     */
    static Observable<Movie> getMovies() throws IOException, JDOMException {
        SAXBuilder builder = new SAXBuilder();
        Document moviesDocument = builder.build(new URL(HBO_GO_MOVIE_LIST_URL));
        Element rootNode = moviesDocument.getRootElement();
        Namespace namespace = rootNode.getNamespace();
        List<Element> moviesElements = rootNode.getChild("body", namespace).getChildren("a", namespace);
        List<Movie> movieList = new ArrayList<Movie>(moviesElements.size());
        for (Element movie: moviesElements) {
            movieList.add(new Movie(movie.getAttributeValue("id"),
                    movie.getAttributeValue("data-t"),
                    movie.getAttributeValue("href")));
        }
        return Observable.from(movieList);
    }

    /**
     * Obtains page ranks for each observed movie, and ranks all movies by totalPageViews.
     */
    private static class MovieObserver implements Observer<Movie> {
        private final List<Movie> rankedMovies = new ArrayList<Movie>(1000);

        @Override
        public void onNext(Movie movie) {
            System.out.print("Processing movie \"" + movie.title);

            // Obtain page view data and sum it from the network source.
            try {
                String json = getJSON(WIKIPEDIA_PAGE_URL_PREFIX + URLEncoder.encode(movie.title, UTF8.asString()), 10000);
                if (json != null) {
                    ObjectMapper m = new ObjectMapper();
                    JsonNode rootNode = m.readTree(json);
                    long totalPageViews = 0;
                    for (Iterator<JsonNode> it = rootNode.path("daily_views").getElements(); it.hasNext(); ) {
                        int dailyPageView = it.next().asInt(-100);
                        if (dailyPageView != -100) {
                            totalPageViews += dailyPageView;
                        }
                    }
                    movie.setTotalPageViews(totalPageViews);
                    rankedMovies.add(movie);
                    System.out.println("\" totalPageViews= "+totalPageViews);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        @Override
        public void onCompleted() {
            Collections.sort(rankedMovies);
            ObjectMapper m = new ObjectMapper();
            ObjectNode root = m.createObjectNode();

            for (Movie movie : rankedMovies) {
                ObjectNode movieNode = root.putObject("movie-" + movie.getId());
                movieNode.put("title", movie.getTitle());
                movieNode.put("id", movie.getId());
                movieNode.put("totalPageViews", movie.getTotalPageViews());
            }

            try {
                m.writeValue(System.out, root);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onError(Throwable e) {
            System.out.println("Received error during movie processing: " + e);
        }
    }

    /**
     * An expensive synchronous blocking call to read a JSON string from a URL.
     * @return {@code null} when there is a problem accessing the JSON data.
     */
    public static String getJSON(String url, int timeout) {
        try {
            URL u = new URL(url);
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.setConnectTimeout(timeout);
            c.setReadTimeout(timeout);
            c.connect();
            int status = c.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    br.close();
                    return sb.toString();
            }

        } catch (Exception ex) {
            return null;
        }
        return null;
    }

    /**
     * A minimal data model based on data in {@link #HBO_GO_MOVIE_LIST_URL} to perform page ranking.
     * Mutable to add page view totals once they are obtained.
     * Comparable so that movies can be ranked by total page views.
     */
    public static class Movie implements Comparable<Movie> {

        String id; //id
        String title; // data-t
        String href; // href;

        long totalPageViews;

        public Movie(String id, String title, String href) {
            this.id = id;
            this.title = title;
            this.href = href;
        }

        public void setTotalPageViews(long totalPageViews) { this.totalPageViews = totalPageViews; }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public long getTotalPageViews() { return totalPageViews; }

        public int compare(Movie o1, Movie o2) {
            if (o1==o2) { return 0; }
            if (o1 != null && o2 == null) { return -1; }
            if (o1 == null) {return 1;}
            if (o1.totalPageViews != o2.totalPageViews) {
                return (int) (o1.totalPageViews - o2.totalPageViews);
            }
            return o1.title.compareTo(o2.title);
        }

        @Override
        public int compareTo(Movie movie) {
            return compare(this, movie);
        }
    }
}

