package com.example.sting.booksearch;

/**
 * {@link Book} contains all the information related to a book
 */
public class Book {

    private String mTitle;
    private String[] mAuthor;
    private String mPublisher;
    private String mPublishedDate;
    private String[] mCategory;
    private String mLanguage;
    private String mImageURL;

    /**
     * Constructor used to create a new {@link Book}
     *
     * @param mTitle         is the title of the book
     * @param mAuthor        is the author of the book
     * @param mPublisher     is the publisher of the book
     * @param mPublishedDate is the date in which the book was published
     * @param mCategory      is the category of the book
     * @param mLanguage      is the language of the book
     * @param mImageURL      is the link for the image thumbnail
     */

    public Book(String mTitle, String[] mAuthor, String mPublisher, String mPublishedDate,
                String[] mCategory, String mLanguage, String mImageURL) {
        this.mTitle = mTitle;
        this.mAuthor = mAuthor;
        this.mPublisher = mPublisher;
        this.mPublishedDate = mPublishedDate;
        this.mCategory = mCategory;
        this.mLanguage = mLanguage;
        this.mImageURL = mImageURL;
    }

    public String getTitle() {
        return this.mTitle;
    }

    public String[] getAuthor() {
        return this.mAuthor;
    }

    public String getPublisher() {
        return this.mPublisher;
    }

    public String getPublishedDate() {
        return this.mPublishedDate;
    }

    public String[] getCategory() {
        return this.mCategory;
    }

    public String getLanguage() {
        return this.mLanguage;
    }

    public String getImageURL() {
        return this.mImageURL;
    }
}
