package com.example.sting.booksearch;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper methods related to requesting and receiving books data from GoogleBooks API.
 */

public final class QueryUtils {

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = QueryUtils.class.getSimpleName();

    /**
     * Query the GoogleBooks API and return a list of {@link Book} objects to populate the list
     * to be displayed,
     */
    public static List<Book> fetchBooksData(String requestUrl) {

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Create URL object
        URL url = createUrl(requestUrl);

        // Perform HTTP request to the URL and receive a JSON response back
        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error closing input stream", e);
        }

        // Extract relevant fields from the JSON response and create an {@link Event} object
        List<Book> books = extractBooksFromJson(jsonResponse);

        // Return the {@link Event}
        return books;
    }

    /**
     * Returns new URL object from the given string URL.
     */
    private static URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error with creating URL ", e);
        }
        return url;
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response.
     */
    private static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        // If the URL is null, then return early.
        if (url == null) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // If the request was successful (response code 200),
            // then read the input stream and parse the response.
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem retrieving the earthquake JSON results.", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    /**
     * Convert the {@link InputStream} into a String which contains the
     * whole JSON response from the server.
     */
    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    /**
     * Return a list {@link Book} objects by parsing out information from the
     * earthquakeJSON string.
     */
    private static List<Book> extractBooksFromJson(String booksJSON) {
        // If the JSON string is empty or null, then return early.
        if (TextUtils.isEmpty(booksJSON)) {
            return null;
        }

        // Create an empty ArrayList that we can start adding books to
        List<Book> books = new ArrayList<>();

        try {
            JSONObject baseJsonResponse = new JSONObject(booksJSON);
            JSONArray itemsArray = baseJsonResponse.getJSONArray("items");

            // If there are results in the items array
            if (itemsArray.length() > 0) {
                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject elementAtIndex = itemsArray.getJSONObject(i);
                    JSONObject volumeInfo = elementAtIndex.getJSONObject("volumeInfo");

                    // Retrieve data for book's title
                    String title = volumeInfo.getString("title");

                    String[] author = new String[0];
                    // Retrieve data for book's authors if there is any
                    if (volumeInfo.has("authors")) {
                        JSONArray authorsArray = volumeInfo.getJSONArray("authors");
                        author = new String[authorsArray.length()];
                        for (int x = 0; x < authorsArray.length(); x++) {
                            author[x] = authorsArray.getString(x);
                        }
                    }

                    String publisher = null;
                    // Retrieve data for book's publisher if there is any
                    if (volumeInfo.has("publisher")) {
                        publisher = volumeInfo.getString("publisher");
                    }

                    String publishedDate = null;
                    // Retrieve data for book's published date if there is any
                    if (volumeInfo.has("publishedDate")) {
                        publishedDate = volumeInfo.getString("publishedDate");
                    }

                    String[] category = new String[0];
                    // Retrieve data for book's categories if there is any
                    if (volumeInfo.has("categories")) {
                        JSONArray categoryArray = volumeInfo.getJSONArray("categories");
                        category = new String[categoryArray.length()];
                        for (int y = 0; y < categoryArray.length(); y++) {
                            category[y] = categoryArray.getString(y);
                        }
                    }

                    // Retrieve data for book's language
                    String language = volumeInfo.getString("language");

                    String imageURL = null;
                    // Retrieve data for book's image if there is any
                    JSONObject images = volumeInfo.optJSONObject("imageLinks");
                    if (images != null) imageURL = images.getString("thumbnail");

                    // Add a new Book to the array
                    books.add(new Book(title, author, publisher, publishedDate, category,
                            language, imageURL));
                }
                return books;
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem parsing the earthquake JSON results", e);
        }
        return null;
    }
}
