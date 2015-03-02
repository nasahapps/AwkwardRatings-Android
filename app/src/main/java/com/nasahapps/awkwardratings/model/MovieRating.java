package com.nasahapps.awkwardratings.model;

import com.parse.ParseObject;

/**
 * Created by Hakeem on 2/28/15.
 */
public class MovieRating {

    private ParseObject movie;
    private boolean rated, awkward;

    public MovieRating(ParseObject movie, boolean rated) {
        this.movie = movie;
        this.rated = rated;
    }

    public MovieRating(ParseObject movie, boolean rated, boolean awkward) {
        this.movie = movie;
        this.rated = rated;
        this.awkward = awkward;
    }

    public ParseObject getMovie() {
        return movie;
    }

    public void setMovie(ParseObject movie) {
        this.movie = movie;
    }

    public boolean isRated() {
        return rated;
    }

    public void setRated(boolean rated) {
        this.rated = rated;
    }

    public boolean isAwkward() {
        return awkward;
    }

    public void setAwkward(boolean awkward) {
        this.awkward = awkward;
    }
}
