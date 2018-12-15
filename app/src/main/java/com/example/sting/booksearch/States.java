package com.example.sting.booksearch;

import java.util.List;

/**
 * {@link States} stores values to be set during runtime, since its values are static they are
 * saved when a screen rotation takes places
 */

public class States {
    public static boolean isBadQuery = false;
    public static boolean isSettingsShowing = false;
    public static boolean isAuthorShowing = false;
    public static boolean isChangeOfPage = false;
    public static int pageNumber = 1;
    public static int indexQueried = 0;
    public static int resultsPerPage = 10;
    public static String queryToSearch = "";
    public static String typeToQuery = "all";
    public static String previousFinalQuery = null;
    public static List<Book> previousBooks = null;
}
