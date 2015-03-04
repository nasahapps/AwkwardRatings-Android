package com.nasahapps.awkwardratings.model;

/**
 * Created by Hakeem on 2/28/15.
 */
public class MovieRating {

    private Number movieId;
    private boolean awkward;

    public MovieRating(Number id, boolean awkward) {
        this.movieId = id;
        this.awkward = awkward;
    }

    public Number getMovieId() {
        return movieId;
    }

    public void setMovieId(Number id) {
        this.movieId = id;
    }

    public boolean isAwkward() {
        return awkward;
    }

    public void setAwkward(boolean awkward) {
        this.awkward = awkward;
    }
}
