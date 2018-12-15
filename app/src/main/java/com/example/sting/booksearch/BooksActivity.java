package com.example.sting.booksearch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.button.MaterialButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
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
    // printType + maxResults + startIndex
    private static final String URL_TO_USE = "https://www.googleapis.com/books/v1/volumes?q=";
    private static final String IN_TITLE = "intitle:";
    private static final String IN_AUTHOR = "inauthor:";
    private static final String MAX_RESULTS = "&maxResults=";
    private static final String PRINT_TYPE = "&printType=";
    private static final String START_INDEX = "&startIndex=";
    private String authorToSearch;
    private String queryToSearch;
    private String finalQuery;
    private String typeToQuery = States.typeToQuery;
    private int pageNumber = States.pageNumber;
    // Can have three values 10,20,40
    private int resultsPerPage = States.resultsPerPage;

    // When a new search is performed its value is set to 0, if the user moves to the next or
    // previous page its value is increased or decreased using resultsPerPage
    private int indexToQuery = States.indexQueried;

    private String printType = PRINT_TYPE + typeToQuery;
    private String maxResults = MAX_RESULTS + String.valueOf(resultsPerPage);
    private String startIndex = START_INDEX + String.valueOf(indexToQuery);

    // Determines whether new content should be loaded.
    private boolean isFirstLoad = true;

    // Determines whether a bad query was provided verifying value in States
    private boolean isBadQuery = States.isBadQuery;

    // Determines whether a change of page or a new query is required verifying value in States
    private boolean isChangeOfPage = States.isChangeOfPage;

    // Determines whether authorEditText is visible or not verifying value in States
    private boolean isAuthorVisible = States.isAuthorShowing;

    // Determines whether settingsScrollView is visible or not verifying value in States
    private boolean isSettingsVisible = States.isSettingsShowing;

    // Determines whether pagesRelativeLayout is visible or not verifying value in States
    private boolean isPagesVisible = States.isPagesShowing;

    // LoaderManager to control the instance of BooksLoader
    LoaderManager loaderManager;

    // Adapter that will populate the books list.
    BooksAdapter adapter;

    // Views in ActionBar
    EditText bookSearchEditText;
    ImageView searchImageView;
    ImageView settingsImageView;

    // Views in main layout
    ListView booksListView;
    ImageView emptyImageView;
    ProgressBar progressBar;

    // Views in settings panel
    ScrollView settingsScrollView;
    AppCompatCheckBox titleCheckBox, authorCheckBox;
    EditText authorEditText;
    AppCompatRadioButton booksRadioButton, magazinesRadioButton, allRadioButton;
    AppCompatSpinner resultPageSpinner;

    // Views in bottom navigation panel
    MaterialButton previousButton, nextButton;
    TextView pageNumberTextView;
    RelativeLayout pagesRelativeLayout;

    // Set of colors for CheckBoxes and RadioButtons
    ColorStateList colorStateList = new ColorStateList(
            new int[][]{
                    new int[]{-android.R.attr.state_checked},
                    new int[]{android.R.attr.state_checked}
            },
            new int[]{
                    Color.rgb(255, 255, 255),
                    Color.rgb(0, 0, 0)
            }
    );

    // Transformation object that allows picasso to resize image according to size of container
    // view
    final Transformation transformation = new Transformation() {
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

    // Name of preferences
    private static final String RESULTS_PER_PAGE = "resultPerPage";

    // Shared Preferences instance
    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_books);
        // Initialize finalQuery if value saved in States is not null,
        // which means there was a search performed before screen rotation
        if (States.previousFinalQuery != null) finalQuery = States.previousFinalQuery;

        // Initialize sharedPreferences
        prefs = this.getPreferences(Context.MODE_PRIVATE);
        editor = prefs.edit();
        // if there is preferences stored, get user's preferences for resultsPerPage
        if (prefs.contains(RESULTS_PER_PAGE))
            resultsPerPage = prefs.getInt(RESULTS_PER_PAGE, resultsPerPage);

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

        // Getting results per page Spinner
        resultPageSpinner = findViewById(R.id.results_spinner);
        final List<Integer> values = new ArrayList<>();
        values.add(10);
        values.add(20);
        values.add(40);
        ArrayAdapter<Integer> adapterSpinner = new ArrayAdapter<>(this,
                R.layout.spinner_text, values);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resultPageSpinner.setAdapter(adapterSpinner);
        // Set spinner to value saved in resultsPerPage
        resultPageSpinner.setSelection(values.indexOf(resultsPerPage));
        resultPageSpinner.setSupportBackgroundTintList(colorStateList);
        resultPageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                resultsPerPage =  Integer.parseInt(resultPageSpinner.getItemAtPosition(i).toString());
                // Save preferences for user's result per pages
                editor.putInt(RESULTS_PER_PAGE, resultsPerPage).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // Getting ImageView to show image when adapter is empty
        emptyImageView = findViewById(R.id.empty_image_view);

        // Getting ProgressBar
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        // Getting settings ScrollView
        settingsScrollView = findViewById(R.id.setting_scroll_view);
        if (isSettingsVisible) settingsScrollView.setVisibility(View.VISIBLE);
        else settingsScrollView.setVisibility(View.GONE);

        // Getting Authors EditText for when the user searches by title and author
        authorEditText = findViewById(R.id.author_edit_text);
        if (isAuthorVisible) authorEditText.setVisibility(View.VISIBLE);
        else authorEditText.setVisibility(View.GONE);
        authorEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    isChangeOfPage = false;
                    States.isChangeOfPage = false;
                    startSearch();
                }
                return false;
            }
        });

        // Getting settings CheckBoxes and RadioButton
        // Setting their color to colorStateList
        titleCheckBox = findViewById(R.id.title_check_box);
        titleCheckBox.setSupportButtonTintList(colorStateList);
        titleCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isAuthorVisible = shouldShowAuthorQuery();
            }
        });
        authorCheckBox = findViewById(R.id.author_check_box);
        authorCheckBox.setSupportButtonTintList(colorStateList);
        authorCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isAuthorVisible = shouldShowAuthorQuery();
            }
        });
        booksRadioButton = findViewById(R.id.books_radio_button);
        booksRadioButton.setSupportButtonTintList(colorStateList);
        magazinesRadioButton = findViewById(R.id.magazine_radio_button);
        magazinesRadioButton.setSupportButtonTintList(colorStateList);
        allRadioButton = findViewById(R.id.all_radio_button);
        allRadioButton.setSupportButtonTintList(colorStateList);

        // Getting views inside the actionbar
        bookSearchEditText = actionBar.getCustomView().findViewById(R.id.book_search_edit_text);
        // Initiates a new search when the GO button is pressed on keyboard
        bookSearchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    isChangeOfPage = false;
                    States.isChangeOfPage = false;
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
                isChangeOfPage = false;
                States.isChangeOfPage = false;
                startSearch();
            }
        });

        settingsImageView = actionBar.getCustomView().findViewById(R.id.settings_image_view);
        settingsImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isSettingsVisible){
                    isSettingsVisible = true;
                    settingsScrollView.setVisibility(View.VISIBLE);
                } else {
                    isSettingsVisible = false;
                    settingsScrollView.setVisibility(View.GONE);
                }
            }
        });

        // Getting the ListView that will display fetched data
        booksListView = findViewById(R.id.books_list_view);
        adapter = new BooksAdapter(this, new ArrayList<Book>());
        booksListView.setAdapter(adapter);
        booksListView.setEmptyView(emptyImageView);

        // Getting bottom bar
        pagesRelativeLayout = findViewById(R.id.pages_relative_layout);
        if (isPagesVisible) pagesRelativeLayout.setVisibility(View.VISIBLE);
        else pagesRelativeLayout.setVisibility(View.GONE);

        // Getting buttons and textView in bottom bar
        pageNumberTextView = findViewById(R.id.page_number_text_view);
        pageNumberTextView.setText(getString(R.string.page) + " " + String.valueOf(pageNumber));
        previousButton = findViewById(R.id.previous_button);
        nextButton = findViewById(R.id.next_button);
        // Moves to next page in the search
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextButton.setClickable(false);
                previousButton.setClickable(false);
                indexToQuery += resultsPerPage;
                States.indexQueried = indexToQuery;
                pageNumber += 1;
                States.pageNumber = pageNumber;
                isChangeOfPage = true;
                States.isChangeOfPage = true;
                startSearch();
            }
        });

        // Moves to previous page in search
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                previousButton.setClickable(false);
                nextButton.setClickable(false);
                // If there is a value higher than 0 in indexToQuery it means more than one page
                // is available and we can move backwards to the previous page
                if (indexToQuery > 0) {
                    indexToQuery -= resultsPerPage;
                    States.indexQueried = indexToQuery;
                    pageNumber -= 1;
                    States.pageNumber = pageNumber;
                    isChangeOfPage = true;
                    States.isChangeOfPage = true;
                    startSearch();
                } else {
                    Toast.makeText(BooksActivity.this,"First page in record",
                            Toast.LENGTH_SHORT).show();
                    nextButton.setClickable(true);
                    previousButton.setClickable(true);
                }
            }
        });

        // Initiating Loader
        startLoader();
    }

    /**
     * Show authorEditText depending on the options selected by the user
     */
    private boolean shouldShowAuthorQuery() {
        if (titleCheckBox.isChecked() && authorCheckBox.isChecked()){
            authorEditText.getText().clear();
            authorEditText.setVisibility(View.VISIBLE);
            return true;
        } else {
            authorEditText.getText().clear();
            authorEditText.setVisibility(View.GONE);
            hideKeyboard();
            return false;
        }
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

        // Remove focus from search and author EditText
        authorEditText.clearFocus();
        bookSearchEditText.clearFocus();

        // Hide keyboard
        hideKeyboard();

        // Check if there is an internet connection
        if (!isConnected()) {
            noInternet();
            return;
        }

        // Retrieve value input in search and author EditText
        queryToSearch = bookSearchEditText.getText().toString().trim();
        authorToSearch = authorEditText.getText().toString().trim();

        // Verify that values are not empty
        // If authorEditText is visible, user needs to input both values before continuing
        // Otherwise user only needs to input a value in the search EditText
        if (isAuthorVisible) {
            if (!TextUtils.isEmpty(authorToSearch) && !TextUtils.isEmpty(queryToSearch)){
                buildFinalQuery();
            } else {
                showToast(getString(R.string.empty_search_author));
            }
        } else {
            if (TextUtils.isEmpty(queryToSearch)) {
                showToast(getString(R.string.empty_search));
            } else {
                buildFinalQuery();
            }
        }
        // Restart LoaderManager
        loaderManager.restartLoader(0, null, BooksActivity.this);
    }

    /**
     * Hides Keyboard
     */
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    /**
     * Show a Toast if input strings are empty
     * @param message is the message to show in toast
      */
    private void showToast(String message) {
        Toast.makeText(BooksActivity.this,message,
                Toast.LENGTH_SHORT).show();
        Picasso.with(BooksActivity.this).load(R.drawable.no_results).into(emptyImageView);
        searchImageView.setClickable(true);

        // Set to true since it is a bad query
        isBadQuery = true;

        pagesRelativeLayout.setVisibility(View.GONE);
        isPagesVisible = false;
    }

    /**
     * Builds finalQuery, to be used as the URL to perform the HTTP request
      */
    private void buildFinalQuery() {
        // Set to true since new content should be loaded
        isFirstLoad = true;

        // Set to false since it is not a bad query
        isBadQuery = false;

        // Clear ImageView in middle of screen
        emptyImageView.setImageResource(0);

        // Show Progress Bar
        progressBar.setVisibility(View.VISIBLE);

        // Convert values into acceptable formats to perform the HTTP request
        queryToSearch = queryToSearch.replaceAll(" ", "%20");
        authorToSearch = authorToSearch.replaceAll(" ","%20");

        if (isAuthorVisible) {
            queryToSearch = IN_TITLE + queryToSearch + "+" + IN_AUTHOR + authorToSearch;
        } else if (titleCheckBox.isChecked()) {
            queryToSearch = IN_TITLE + queryToSearch;
        } else if (authorCheckBox.isChecked()) {
            queryToSearch = IN_AUTHOR + queryToSearch;
        }

        // Set typeToQuery to selected value
        if (booksRadioButton.isChecked()) typeToQuery = getString(R.string.setting_book);
        if (magazinesRadioButton.isChecked()) typeToQuery = getString(R.string.setting_magazine);
        if (allRadioButton.isChecked()) typeToQuery = getString(R.string.setting_all);


        // Compare current query with previously searched query
        if (!TextUtils.equals(queryToSearch, States.queryToSearch)) {
            // If different replace value in States with new value and reset query numbers
            States.queryToSearch = queryToSearch;
            resetQueryNumbers();
        } else {
            if (!isChangeOfPage)
                // If same query is requested but no change of page is needed then only
                // reset query numbers
                resetQueryNumbers();
            if (isChangeOfPage && (States.resultsPerPage != resultsPerPage ||
                    !TextUtils.equals(States.typeToQuery, typeToQuery))) {
                // If same query is requested and search parameters have changed then perform
                // new search and reset query numbers
                Toast.makeText(BooksActivity.this,
                        "Search parameters changed, performing new search", Toast.LENGTH_SHORT).show();
                resetQueryNumbers();
                isChangeOfPage = false;
                States.isChangeOfPage = false;
            }
        }

        // Save query numbers to States
        States.resultsPerPage = resultsPerPage;
        States.typeToQuery = typeToQuery;

        // Build finalQuery
        maxResults = MAX_RESULTS + resultsPerPage;
        startIndex = START_INDEX + indexToQuery;
        printType = PRINT_TYPE + typeToQuery;
        finalQuery = URL_TO_USE + queryToSearch + printType + maxResults + startIndex;
    }

    /**
     * Resets query numbers
     */
    private void resetQueryNumbers() {
        indexToQuery = 0;
        States.indexQueried = indexToQuery;
        pageNumber = 1;
        States.pageNumber = pageNumber;
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
        // Will get called twice, that situation can be taken advantage of when loading the data
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
            pageNumberTextView.setText(getString(R.string.page) + " " + String.valueOf(pageNumber));

            // Save value of finalQuery in States
            States.previousFinalQuery = finalQuery;

            // Save value of books in States
            States.previousBooks = books;

            nextButton.setClickable(true);
            previousButton.setClickable(true);
            pagesRelativeLayout.setVisibility(View.VISIBLE);
            isPagesVisible = true;
        } else if (finalQuery == null && books == null && !isBadQuery && !BooksLoader.BAD_RESPONSE) {
            // It means that it is the first run so there is no data to be shown
            Picasso.with(BooksActivity.this).load(R.drawable.search_book).into(emptyImageView);

            nextButton.setClickable(false);
            previousButton.setClickable(false);
            pageNumberTextView.setText(getString(R.string.page));
            pagesRelativeLayout.setVisibility(View.GONE);
            isPagesVisible = false;
        } else if (isChangeOfPage && books == null) {
            // Last page in record is already showing and records can move forward
            Toast.makeText(BooksActivity.this, "Last page in record", Toast.LENGTH_SHORT).show();

            // Retrieve value from States so that when loader is restarted it has a URL to perform
            // the HTTP request
            finalQuery = States.previousFinalQuery;

            // Pass value of books saved in States to the adapter so previous info can be shown
            adapter.addAll(States.previousBooks);

            indexToQuery -= resultsPerPage;
            pageNumber -= 1;
            pageNumberTextView.setText(getString(R.string.page) + " " + String.valueOf(pageNumber));
            States.indexQueried = indexToQuery;
            States.pageNumber = pageNumber;
            nextButton.setClickable(true);
            previousButton.setClickable(true);
            pagesRelativeLayout.setVisibility(View.VISIBLE);
            isPagesVisible = true;

            // Restart Loader
            loaderManager.restartLoader(0, null, BooksActivity.this);
        } else {
            // Bad query to perform HTTP request
            Picasso.with(BooksActivity.this).load(R.drawable.no_results).into(emptyImageView);

            // Set to true since it is a bad query
            isBadQuery = true;

            // Saved value of finalQuery in States
            States.previousFinalQuery = finalQuery;

            // Clear value of books in States
            States.previousBooks = null;

            nextButton.setClickable(false);
            previousButton.setClickable(false);
            pagesRelativeLayout.setVisibility(View.GONE);
            isPagesVisible = false;
            pageNumberTextView.setText(getString(R.string.page));
            resetQueryNumbers();
            loaderManager.restartLoader(0, null, BooksActivity.this);
        }

        // Once data is shown make search button clickable again
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
               Picasso.with(BooksActivity.this).load(R.drawable.no_internet_connection)
                                .transform(transformation).into(emptyImageView);
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
        // Save value in States so different parameters are not lost when screen rotates
        States.isBadQuery = isBadQuery;
        States.isSettingsShowing = isSettingsVisible;
        States.isAuthorShowing = isAuthorVisible;
        States.isPagesShowing = isPagesVisible;
    }
}
