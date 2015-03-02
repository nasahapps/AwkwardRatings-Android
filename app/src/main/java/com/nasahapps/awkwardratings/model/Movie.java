package com.nasahapps.awkwardratings.model;

/**
 * Created by Hakeem on 2/28/15.
 *
 * Automatically deserialized with GSON by Retrofit
 */
public class Movie {

    private boolean adult;
    private String backdrop_path, original_title, title, release_date, poster_path;
    private int id;

    public boolean isAdult() {
        return adult;
    }

    public String getBackdropPath() {
        return backdrop_path;
    }

    public String getOriginalTitle() {
        return original_title;
    }

    public String getTitle() {
        return title;
    }

    public String getReleaseDate() {
        return release_date;
    }

    public String getPosterPath() {
        return poster_path;
    }

    public int getId() {
        return id;
    }
}
