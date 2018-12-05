package com.example.sting.booksearch;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.util.ArrayList;
import java.util.List;

/**
 * Main page that will display information about the books searched by user, the HTTP request to
 * obtain that data is performed by initiating a {@link BooksLoader} instance that will return a
 * list of {@link Book} objects.
 */

public class BooksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<Book>> {

    // Strings that will come together to form a valid URL (finalQuery) that will be used to
    // perform the HTTP request, to create finalQuery the following happens URL_TO_USE + queryToSearch +
    // MAX_RESULTS
    private static final String URL_TO_USE = "https://www.googleapis.com/books/v1/volumes?q=";
    private static final String MAX_RESULTS = "&maxResults=40";
    private String queryToSearch;
    private String finalQuery;

    // Name of key for preferences
    private static final String BAD_QUERY = "badQuery";
    private static final String FIRST_LOAD = "firstLoad";

    // Determines whether new content should be loaded.
    private boolean isFirstLoad = true;

    // Determines whether a bad query was provided
    private boolean isBadQuery = false;

    // LoaderManager to control the instance of BooksLoader
    LoaderManager loaderManager;

    // Adapter that will populate the books list.
    BooksAdapter adapter;

    // Views in ActionBar
    EditText bookSearchEditText;
    ImageView searchImageView;

    // Views in main layout
    ListView booksListView;
    ImageView emptyImageView;
    ProgressBar progressBar;

    // Transformation object that allows picasso to resize image according to size of container
    // view

    Transformation transformation = new Transformation() {
        @Override
        public Bitmap transform(Bitmap source) {

            int targetWidth = emptyImageView.getWidth();
            int targetHeight = emptyImageView.getHeight();

            Bitmap result = Bitmap.createScaledBitmap(source, targetWidth, targetHeight,
                    false);
            if (result != source) {
                // Same bitmap is returned if sizes are the same
                source.recycle();
            }
            return result;
        }

        @Override
        public String key() {
            return "transformation";
        }
    };

    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_books);

        sharedPrefs = this.getPreferences(Context.MODE_PRIVATE);
        editor = sharedPrefs.edit();

        if (sharedPrefs.contains(BAD_QUERY)) isBadQuery = sharedPrefs.getBoolean(BAD_QUERY,false);

        Log.i("Information", "onCreate() called");

        // Getting action bar to set custom view, allows user to search for a book with the given
        // input
        ActionBar actionBar = getSupportActionBar();
        View customView = getLayoutInflater().inflate(R.layout.search_action_bar, null);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(customView);

        // Creating a toolbar to eliminate unnecessary spacing on the sides of customView
        Toolbar parent = (Toolbar) customView.getParent();
        parent.setPadding(0, 0, 0, 0);
        parent.setContentInsetsAbsolute(0, 0);

        // Getting TextView to show empty text
        emptyImageView = findViewById(R.id.empty_image_view);

        // Getting ProgressBar
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        // Getting views inside the actionbar
        bookSearchEditText = actionBar.getCustomView().findViewById(R.id.book_search_edit_text);
        // Initiates a new search when the GO button is pressed on keyboard
        bookSearchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    startSearch();
                }
                return false;
            }
        });


        searchImageView = actionBar.getCustomView().findViewById(R.id.search_image_view);
        // Initiates a new search when the image is clicked
        searchImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSearch();
            }
        });

        // Getting the ListView that will display fetched data
        booksListView = findViewById(R.id.books_list_view);
        adapter = new BooksAdapter(this, new ArrayList<Book>());
        booksListView.setAdapter(adapter);
        booksListView.setEmptyView(emptyImageView);

        // Initiating Loader
        startLoader();
    }

    /**
     * Perform a new search by restarting the instance of {@link BooksLoader}, it sets the value
     * of finalQuery to be passed as new valid URL that will be used to perform the HTTP request
     */
    private void startSearch() {
        // Clear current data
        adapter.clear();

        // Prevent user from starting process again until search is completed
        searchImageView.setClickable(false);

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        // Remove focus from search EditText
        bookSearchEditText.clearFocus();

        // Check if there is an internet connection
        if (!isConnected()) {
            noInternet();
            return;
        }

        // Retrieve value input in Search EditText
        queryToSearch = bookSearchEditText.getText().toString().trim();

        // Verify that value is not empty
        if (TextUtils.isEmpty(queryToSearch)) {
            Toast.makeText(BooksActivity.this, getString(R.string.enter_search),
                    Toast.LENGTH_SHORT).show();
            emptyImageView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Picasso.with(BooksActivity.this).load(R.drawable.no_results).fit().
                            centerCrop().into(emptyImageView);
                }
            },0);
            searchImageView.setClickable(true);

            // Set to true since it is a bad query
            isBadQuery = true;
            editor.putBoolean(BAD_QUERY,isBadQuery).commit();
        } else {
            // Set to true since new content should be loaded
            isFirstLoad = true;

            // Set to false since it is not a bad query
            isBadQuery = false;
            editor.putBoolean(BAD_QUERY,isBadQuery).commit();

            emptyImageView.setImageResource(0);
            // Show Progress Bar
            progressBar.setVisibility(View.VISIBLE);

            // Build finalQuery and restart LoaderManager
            queryToSearch = queryToSearch.replaceAll(" ", "%20");
            finalQuery = URL_TO_USE + queryToSearch + MAX_RESULTS;
        }
        loaderManager.restartLoader(0, null, BooksActivity.this);
    }

    /**
     * Starts a new {@link BooksLoader}
     */
    private void startLoader() {
        loaderManager = LoaderManager.getInstance(BooksActivity.this);
        loaderManager.initLoader(0, null, BooksActivity.this).forceLoad();
    }


    @Override
    public Loader<List<Book>> onCreateLoader(int i, Bundle bundle) {
        // Returns a new BooksLoader by passing finalQuery to it
        if (isBadQuery) return new BooksLoader(BooksActivity.this,null);
        else return new BooksLoader(BooksActivity.this, finalQuery);
    }

    @Override
    public void onLoadFinished(Loader<List<Book>> loader, List<Book> books) {
        Log.i("information", "onLoadFinished() called");
        // Execute only if new content should be loaded
        if (isFirstLoad) {
            updateUI(books);

            // Set new content loading to false
            isFirstLoad = false;
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Book>> loader) {
        Log.i("information", "onLoaderReset() called");
        // Clears up current set of data and informs loader of new values passed to it
        adapter.clear();
        loader.commitContentChanged();
    }

    /**
     * Updates the UI to be displayed
     *
     * @param books is a list of {@link Book} returned by {@link BooksLoader}
     */
    public void updateUI(List<Book> books) {
        // Hide Progress Bar
        progressBar.setVisibility(View.GONE);

        // Execute if there is no internet connection
        if (!isConnected()) {
            noInternet();
            return;
        }

        // Check possible scenarios
        if (books != null) {
            // Set new data to be displayed
            adapter.clear();
            adapter.addAll(books);
        } else if (finalQuery == null && books == null && !isBadQuery && BooksLoader.bad_Response) {
            // It means that it is the first run so there is no data to be shown
            emptyImageView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Picasso.with(BooksActivity.this).load(R.drawable.start_search).transform(transformation)
                            .into(emptyImageView);
                }
            },0);
        } else {
            // Bad query to perform HTTP request
            emptyImageView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Picasso.with(BooksActivity.this).load(R.drawable.no_results).fit().
                            centerCrop().into(emptyImageView);
                }
            },0);

            // Set to true since it is a bad query
            isBadQuery = true;
            editor.putBoolean(BAD_QUERY,isBadQuery).commit();

            loaderManager.restartLoader(0, null, BooksActivity.this);
        }

        // Once data is shown make Search image clickable again
        searchImageView.setClickable(true);
    }

    /**
     * Runs when there is no internet connection while attempting to perform a query
     */
    private void noInternet() {
        emptyImageView.setImageResource(0);
        progressBar.setVisibility(View.VISIBLE);
        emptyImageView.postDelayed(new Runnable() {
            @Override
            public void run() {
               Picasso.with(BooksActivity.this).load(R.drawable.no_internet_connection).
                                fit().centerCrop().into(emptyImageView);
               searchImageView.setClickable(true);
               progressBar.setVisibility(View.GONE);
            }
        }, 1000);
    }

    /**
     * Verifies that there is an internet connection
     *
     * @return true if connection is active, false otherwise
     */
    private boolean isConnected() {
        // Check for connectivity status
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Set value to false to it can be restarted next time app is launched
        isBadQuery = false;
        editor.putBoolean(BAD_QUERY,isBadQuery).commit();
    }
}
