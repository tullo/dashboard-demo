/**
 * DISCLAIMER
 *
 * The quality of the code is such that you should not copy any of it as best
 * practice how to build Vaadin applications.
 *
 * @author jouni@vaadin.com
 *
 */
package com.vaadin.demo.dashboard.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataProvider {

    private static final String FILE_NAME = "movies.txt";
    private static final String MOVIES_URL = "http://api.rottentomatoes.com/api/public/v1.0/lists/movies/in_theaters.json";

    private static final Random rand = new Random();

    /* List of movies playing currently in theaters */
    private static final ArrayList<Movie> movies = new ArrayList<>();

    /* List of countries and cities for them */
    private static HashMap<String, ArrayList<String>> countryToCities = new HashMap<>();

    /* List of theaters */
    private static final List<String> theaters = new ArrayList<String>() {
        {
            add("Threater 1");
            add("Threater 2");
            add("Threater 3");
            add("Threater 4");
            add("Threater 5");
            add("Threater 6");
        }
    };

    /* List of rooms */
    private static final List<String> rooms = new ArrayList<String>() {
        {
            add("Room 1");
            add("Room 2");
            add("Room 3");
            add("Room 4");
            add("Room 5");
            add("Room 6");
        }
    };

    /**
     * =========================================================================
     * Transactions data, used in tables and graphs
     * =========================================================================
     */

    /* Container with all the transactions */
    private TransactionsContainer transactions;
    private static double totalSum = 0;

    public static void reseed() {
        rand.setSeed(1L);
    }

    /**
     * Initialize the data for this application.
     *
     * @throws java.io.IOException
     */
    public DataProvider() {
        reseed();
        loadMoviesData();
        loadTheaterData();
        generateTransactionsData();
    }

    /**
     * =========================================================================
     * Movies in theaters
     * =========================================================================
     */

    /* Simple Movie class */
    public static class Movie {

        public final String title;
        public final String synopsis;
        public final String thumbUrl;
        public final String posterUrl;
        /**
         * Duration in minutes
         */
        public final int duration;
        public Date releaseDate = null;

        public int score;
        public double sortScore = 0;

        Movie(String title, String synopsis, String thumbUrl, String posterUrl,
                JsonObject releaseDates, JsonObject critics) {
            this.title = title;
            this.synopsis = synopsis;
            this.thumbUrl = thumbUrl;
            this.posterUrl = posterUrl;
            this.duration = (int) ((1 + Math.round(rand.nextDouble())) * 60 + 45 + rand.nextInt(30));

            try {
                String datestr = releaseDates.get("theater").getAsString();
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                releaseDate = df.parse(datestr);
                score = critics.get("critics_score").getAsInt();
                sortScore = 0.6 / (0.01 + (System.currentTimeMillis() - releaseDate
                        .getTime()) / (1000 * 60 * 60 * 24 * 5));
                sortScore += 10.0 / (101 - score);
            } catch (ParseException e) {
                e.printStackTrace();
            }

        }

        public String titleSlug() {
            return title.toLowerCase().replace(' ', '-').replace(":", "")
                    .replace("'", "").replace(",", "").replace(".", "");
        }

        public void reCalculateSortScore(Calendar cal) {
            if (cal.before(releaseDate)) {
                sortScore = 0;
                return;
            }
            sortScore = 0.6 / (0.01 + (cal.getTimeInMillis() - releaseDate
                    .getTime()) / (1000 * 60 * 60 * 24 * 5));
            sortScore += 10.0 / (101 - score);
        }
    }

    /**
     * Get a list of movies currently playing in theaters.
     *
     * @return a list of Movie objects
     */
    public static ArrayList<Movie> getMovies() {
        return movies;
    }

    private static String readMoviesFromCache(Path path) throws IOException {
        return new String(Files.readAllBytes(path));
    }

    private static JsonObject toJsonObject(String jsonText) throws IOException {
        JsonElement jelement = new JsonParser().parse(jsonText.toString());
        return jelement.getAsJsonObject();
    }
    
    private static String readMoviesFromServiceProvider(String url) throws IOException {
        StringBuilder sb = new StringBuilder(2000);
        try ( // try-with-resources
            InputStream is = new URL(url).openStream();
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(isr)) {
            reader.lines().forEach(line -> sb.append(line));
        }
        return sb.toString();
    }

    /**
     * Initialize the list of movies playing in theaters currently. Uses a
     * stored response from the Rotten Tomatoes API for ensuring the same data
     * when testing. The response is cached in a local file for 24h (daily limit
     * of API calls is 10,000).
     */
    static void loadMoviesData() {
        
        long oneDayInMillis = 1000 * 60 * 60 * 24;
        
        String cachedJsonTxt = null;
        JsonObject jsonObj = null;

        try {
            System.out.println("*****" + cacheLocationPath().toAbsolutePath());
            FileTime now = FileTime.fromMillis(System.currentTimeMillis());
            FileTime lastMod = Files.getLastModifiedTime(cacheLocationPath());
            FileTime cacheTimeout = FileTime.fromMillis(lastMod.toMillis() + oneDayInMillis);

            if (cacheHasExpired(cacheTimeout, now)) {
                // cache expired
                cachedJsonTxt = fetchAndCacheMovies();
            } else {
                cachedJsonTxt = readMoviesFromCache(cacheLocationPath());
                if (cachedJsonTxt.isEmpty()) {
                    cachedJsonTxt = fetchAndCacheMovies();
                }
            }
            jsonObj = toJsonObject(cachedJsonTxt);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException ex) {
            Logger.getLogger(DataProvider.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (jsonObj == null) {
            return;
        }

        JsonArray moviesJson;
        movies.clear();
        moviesJson = jsonObj.getAsJsonArray("movies");
        for (int i = 0; i < moviesJson.size(); i++) {
            JsonObject movieJson = moviesJson.get(i).getAsJsonObject();
            JsonObject posters = movieJson.get("posters").getAsJsonObject();
            String profile = posters.get("profile").getAsString();
            if (!profile.contains("poster_default")) {
                String detailed = posters.get("detailed").getAsString();
                movies.add(populateMovie(movieJson, profile, detailed));
            }
        }
    }

    private static Movie populateMovie(JsonObject obj, String profile, String detailed) {
        Movie movie = new Movie(
                obj.get("title").getAsString(),
                obj.get("synopsis").getAsString(), 
                profile, detailed,
                obj.get("release_dates").getAsJsonObject(),
                obj.get("ratings").getAsJsonObject());
        return movie;
    }
    
    /**
     * Fetches movie data via Rotten Tomatoes API and stores the response
     * locally.
     *
     * The Rotten Tomatoes daily limit of API calls is 10,000.
     *
     * @return response as JSON text
     * @throws IOException
     */
    private static String fetchAndCacheMovies() throws IOException, URISyntaxException {
        // Get an API key from http://developer.rottentomatoes.com
        String apiKey = System.getProperty("rottentomatoes_apikey", "xxxxxxxxxxxxxxxxxxx");
        String movies = readMoviesFromServiceProvider(MOVIES_URL + "?page_limit=30&apikey=" + apiKey);
        Files.write(cacheLocationPath(), movies.getBytes(StandardCharsets.UTF_8));
        return movies;
    }

    private static Path cacheLocationPath() throws URISyntaxException {
        //DataProvider.class.getResource("/") => /WEB-INF/classes/
        return Paths.get(DataProvider.class.getResource(FILE_NAME).toURI());
    }

    private static boolean cacheHasExpired(FileTime cacheTime, FileTime now) {
        return 0 > cacheTime.compareTo(now);
    }

    /* Parse the list of countries and cities */
    private static HashMap<String, ArrayList<String>> loadTheaterData() {

        /* First, read the text file into a list of strings */
        List<String> countryList = new LinkedList<>();
        try ( // try-with-resources
                InputStream is = DataProvider.class.getResourceAsStream("cities.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            reader.lines().forEach(line -> countryList.add(line));
        } catch (IOException ex) {
            Logger.getLogger(DataProvider.class.getName()).log(Level.SEVERE, null, ex);
        }

        /* The list has rows with tab delimited values */
        countryToCities = new HashMap<>();
        for (String line : countryList) {
            String[] tabs = line.split("\t");
            String city = tabs[1];
            String country = tabs[6];

            if (!countryToCities.containsKey(country)) {
                countryToCities.put(country, new ArrayList<>());
            }
            countryToCities.get(country).add(city);
        }

        return countryToCities;
    }

    public TransactionsContainer getTransactions() {
        return transactions;
    }

    /* Create a list of dummy transactions */
    private void generateTransactionsData() {
        GregorianCalendar today = new GregorianCalendar();

        /*
         * Data items: timestamp, country, city, theater, room, movie title,
         * number of seats, price
         */
        transactions = new TransactionsContainer();

        /* Amount of items to create initially */
        for (int i = 1000; i > 0; i--) {
            // Start from 1st of current month
            GregorianCalendar c = new GregorianCalendar();

            // we will go at most 4 months back
            int newMonthSubstractor = (int) (5.0 * rand.nextDouble());
            c.add(Calendar.MONTH, -newMonthSubstractor);

            int newDay = (int) (1 + (int) (30.0 * rand.nextDouble()));
            c.set(Calendar.DAY_OF_MONTH, newDay);

            if (today.before(c)) {
                newDay = (int) (1 + (int) (today.get(Calendar.DAY_OF_MONTH) * rand
                        .nextDouble()));
                c.set(Calendar.DAY_OF_MONTH, newDay);
            }

            // Randomize time of day
            c.set(Calendar.HOUR, (int) (rand.nextDouble() * 24.0));
            c.set(Calendar.MINUTE, (int) (rand.nextDouble() * 60.0));
            c.set(Calendar.SECOND, (int) (rand.nextDouble() * 60.0));
            createTransaction(c);
            // System.out.println(df.format(c.getTime()));
        }
        transactions.sort(new String[]{"timestamp"}, new boolean[]{true});
        updateTotalSum();
    }

    private void updateTotalSum() {
        totalSum = 0;
        for (Object id : transactions.getItemIds()) {
            Item item = transactions.getItem(id);
            Object value = item.getItemProperty("Price").getValue();
            totalSum += Double.parseDouble(value.toString());
        }
    }

    public static double getTotalSum() {
        return totalSum;
    }

    private void createTransaction(Calendar cal) {

        if (movies.isEmpty()) {
            // nothing to do
            return;
        }

        // Movie sort scores
        movies.stream().forEach((m) -> {
            m.reCalculateSortScore(cal);
        });

        Collections.sort(movies, new Comparator<Movie>() {
            @Override
            public int compare(Movie o1, Movie o2) {
                return (int) (100.0 * (o2.sortScore - o1.sortScore));
            }
        });

        String country =  new RandomIterator<String>(countryToCities.keySet()).next();
        // City
        ArrayList<String> cities = countryToCities.get(country);
        String city = cities.get(0);

        // Theater
        String theater = theaters.get((int) (rand.nextDouble() * (theaters.size() - 1)));

        // Room
        String room = rooms.get((int) (rand.nextDouble() * (rooms.size() - 1)));

        // Title
        int randomIndex = (int) (Math.abs(rand.nextGaussian()) * (movies.size() / 2.0 - 1));
        while (randomIndex >= movies.size()) {
            randomIndex = (int) (Math.abs(rand.nextGaussian()) * (movies.size() / 2.0 - 1));
        }
        if (movies.get(randomIndex).releaseDate.compareTo(cal.getTime()) >= 0) {
            // System.out.println("skipped " + movies.get(randomIndex).title);
            // System.out.println(df.format(movies.get(randomIndex).releaseDate));
            // System.out.println(df.format(cal.getTime()));
            // System.out.println();
            // ++skippedCount;
            // System.out.println(skippedCount);
            return;
        }

        // Seats
        int seats = (int) (1 + rand.nextDouble() * 3);

        // Price (approx. USD)
        double price = (double) (seats * (6 + (rand.nextDouble() * 3)));

        final String title = movies.get(randomIndex).title;
        transactions.addTransaction(cal, country, city, theater, room, title, seats, price);

        // revenue.add(cal.getTime(), title, price);
    }

    public IndexedContainer getRevenueForTitle(String title) {
        // System.out.println(title);
        IndexedContainer revenue = new IndexedContainer();
        revenue.addContainerProperty("timestamp", Date.class, new Date());
        revenue.addContainerProperty("revenue", Double.class, 0.0);
        revenue.addContainerProperty("date", String.class, "");
        int index = 0;
        for (Object id : transactions.getItemIds()) {
            SimpleDateFormat df = new SimpleDateFormat();
            df.applyPattern("MM/dd/yyyy");

            Item item = transactions.getItem(id);

            if (title.equals(item.getItemProperty("Title").getValue())) {
                Date d = (Date) item.getItemProperty("timestamp").getValue();

                Item i = revenue.getItem(df.format(d));
                if (i == null) {
                    i = revenue.addItem(df.format(d));
                    i.getItemProperty("timestamp").setValue(d);
                    i.getItemProperty("date").setValue(df.format(d));
                }
                double current = (Double) i.getItemProperty("revenue")
                        .getValue();
                current += (Double) item.getItemProperty("Price").getValue();

                i.getItemProperty("revenue").setValue(current);
            }
        }

        revenue.sort(new Object[]{"timestamp"}, new boolean[]{true});
        return revenue;
    }

    public IndexedContainer getRevenueByTitle() {
        IndexedContainer revenue = new IndexedContainer();
        revenue.addContainerProperty("Title", String.class, "");
        revenue.addContainerProperty("Revenue", Double.class, 0.0);

        for (Object id : transactions.getItemIds()) {

            Item item = transactions.getItem(id);

            String title = item.getItemProperty("Title").getValue().toString();

            if (title == null || "".equals(title)) {
                continue;
            }

            Item i = revenue.getItem(title);
            if (i == null) {
                i = revenue.addItem(title);
                i.getItemProperty("Title").setValue(title);
            }
            double current = (Double) i.getItemProperty("Revenue").getValue();
            current += (Double) item.getItemProperty("Price").getValue();
            i.getItemProperty("Revenue").setValue(current);
        }

        revenue.sort(new Object[]{"Revenue"}, new boolean[]{false});

        // TODO sometimes causes and IndexOutOfBoundsException
        if (revenue.getItemIds().size() > 10) {
            // Truncate to top 10 items
            List<Object> remove = new ArrayList<>();
            revenue.getItemIds(10, revenue.getItemIds().size()).stream().forEach((id) -> {
                remove.add(id);
            });
            remove.stream().forEach((id) -> {
                revenue.removeItem(id);
            });
        }

        return revenue;
    }

    public static Movie getMovieForTitle(String title) {
        for (Movie movie : movies) {
            if (movie.title.equals(title)) {
                return movie;
            }
        }
        return null;
    }

}
