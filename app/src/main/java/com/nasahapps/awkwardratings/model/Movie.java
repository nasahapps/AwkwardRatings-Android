package com.nasahapps.awkwardratings.model;

import java.util.List;

/**
 * Created by Hakeem on 2/28/15.
 * <p/>
 * Automatically deserialized with GSON by Retrofit
 */
public class Movie {

    private boolean adult;
    private String backdrop_path, original_title, title, release_date, poster_path, overview;
    private int id, budget, revenue, runtime;
    private List<Genre> genres;
    private VideoResults videos;
    private KeywordResults keywords;

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

    public String getOverview() {
        return overview;
    }

    public int getBudget() {
        return budget;
    }

    public int getRevenue() {
        return revenue;
    }

    public int getRuntime() {
        return runtime;
    }

    public List<Genre> getGenres() {
        return genres;
    }

    public VideoResults getVideos() {
        return videos;
    }

    public KeywordResults getKeywords() {
        return keywords;
    }
}
