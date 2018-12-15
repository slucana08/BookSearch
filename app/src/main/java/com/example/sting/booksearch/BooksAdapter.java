package com.example.sting.booksearch;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.support.design.widget.TabLayout;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.util.List;

/**
 * {@link BooksAdapter} allows you to display data in a ListView using a list of {@link Book}
 */

public class BooksAdapter extends ArrayAdapter<Book> {

    /**
     * Constructor that allow you to create new {@link BooksAdapter}
     *
     * @param context is the right context
     * @param books   is a list of {@link Book}
     */
    public BooksAdapter(Context context, List<Book> books) {
        super(context, 0, books);
    }

    // Returns information about the books to be displayed in a ListView item according to
    // the given position
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).
                    inflate(R.layout.list_item, parent, false);
        }

        // Get a Book object to get information from
        final Book currentBook = getItem(position);

        // Load up book's title
        TextView titleTextView = listItemView.findViewById(R.id.title_text_view);
        titleTextView.setText(currentBook.getTitle());

        // Load up book's authors if there is any
        TextView authorTextView = listItemView.findViewById(R.id.author_text_view);
        String[] authors = currentBook.getAuthor();
        String author = getContext().getResources().getString(R.string.not_available);
        if (authors.length != 0) {
            author = "";
            for (int i = 0; i < authors.length; i++) {
                if (i == authors.length - 1) author += authors[i];
                else author += authors[i] + ", ";
            }
        }
        authorTextView.setText(author);

        // Load up book's publisher and published date
        TextView publishedTextView = listItemView.findViewById(R.id.published_text_view);
        if (currentBook.getPublisher() == null && currentBook.getPublishedDate() == null) {
            publishedTextView.setText(getContext().getResources().getString(R.string.not_available));
        } else if (currentBook.getPublisher() == null) {
            publishedTextView.setText(currentBook.getPublishedDate());
        } else if (currentBook.getPublishedDate() == null) {
            publishedTextView.setText(currentBook.getPublisher());
        } else {
            publishedTextView.setText(currentBook.getPublisher() + " - " + currentBook.getPublishedDate());
        }

        // Load up book's categories if there is any
        TextView categoryTextView = listItemView.findViewById(R.id.category_text_view);
        String[] categories = currentBook.getCategory();
        String category = getContext().getResources().getString(R.string.not_available);
        if (categories.length != 0) {
            category = "";
            for (int i = 0; i < categories.length; i++) {
                if (i == categories.length - 1) category += categories[i];
                else category += categories[i] + ", ";
            }
        }
        categoryTextView.setText(category);

        // Load up book's language
        TextView languageTextView = listItemView.findViewById(R.id.language_text_view);
        languageTextView.setText(currentBook.getLanguage());

        // Load up the book's image if there is any
        final ImageView thumbnailImageView = listItemView.findViewById(R.id.thumbnail_image_view);

        // Transformation object that allows picasso to resize image according to size of container
        // view
        final Transformation transformation = new Transformation() {
            @Override
            public Bitmap transform(Bitmap source) {

                int targetWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,120,
                        getContext().getResources().getDisplayMetrics() );
                int targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,180,
                        getContext().getResources().getDisplayMetrics() );

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

        if (currentBook.getImageURL() != null) {
            Picasso.with(thumbnailImageView.getContext()).load(currentBook.getImageURL()).placeholder(R.drawable.book_icon).
                    transform(transformation).into(thumbnailImageView);
        } else {
            Picasso.with(thumbnailImageView.getContext()).load(R.drawable.book_icon).transform(transformation).
                    into(thumbnailImageView);
        }

        return listItemView;
    }
}
