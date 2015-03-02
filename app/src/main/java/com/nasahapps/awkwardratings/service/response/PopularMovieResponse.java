package com.nasahapps.awkwardratings.service.response;

import com.nasahapps.awkwardratings.model.Movie;

import java.util.List;

/**
 * Created by Hakeem on 2/28/15.
 *
 * Automatically deserialized with GSON by Retrofit
 */
public class PopularMovieResponse {

    private int page, total_pages, total_results;
    private List<Movie> results;

    public int getPage() {
        return page;
    }

    public int getTotalPages() {
        return total_pages;
    }

    public int getTotalResults() {
        return total_results;
    }

    public List<Movie> getResults() {
        return results;
    }
}
