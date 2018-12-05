package com.example.sting.booksearch;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import java.util.List;

/**
 * {@link BooksLoader} returns a list of {@link Book} that was fetched by performing an HTTP request
 * in the background thread
 */

public class BooksLoader extends AsyncTaskLoader<List<Book>> {

    private String mURL;

    // Determines whether there was a bad response while performing the HTTP request
    public static boolean bad_Response = false;

    /**
     * Constructor that allows you to instantiate a new {@link BooksLoader}
     *
     * @param context is the right context
     * @param mURL    is the URL to perform the HTTP request
     */
    public BooksLoader(Context context, String mURL) {
        super(context);
        this.mURL = mURL;
    }

    @Override
    protected void onStartLoading() {
        Log.i("information", "onStartLoading() called");
        // Execute only if there is an internet connection
        if (isConnected()) forceLoad();
    }

    public List<Book> loadInBackground() {
        Log.i("Information", "loadInBackground() called");
        // Execute if there is no URL provided
        if (mURL == null) return null;

        // Perform HTTP request if there is a URL provided
        List<Book> books = QueryUtils.fetchBooksData(mURL);

        // If result is null it means HTTP request was unsuccessful
        if (books == null) bad_Response = true;

        return books;
    }

    /**
     * Verifies that there is an internet connection
     *
     * @return true if connection is active, false otherwise
     */
    private boolean isConnected() {
        // Check for connectivity status
        ConnectivityManager cm = (ConnectivityManager) getContext().
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

}
