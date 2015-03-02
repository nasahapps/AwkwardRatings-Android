package com.nasahapps.awkwardratings.model;

/**
 * Created by Hakeem on 2/28/15.
 */
public class MovieRating {

    private Movie movie;
    private String movieId;
    private boolean rated, awkward;

    public MovieRating(String id, boolean rated) {
        this.movieId = id;
        this.rated = rated;
    }

    public MovieRating(String id, boolean rated, boolean awkward) {
        this.movieId = id;
        this.rated = rated;
        this.awkward = awkward;
    }

    public MovieRating(Movie movie, boolean rated) {
        this.movie = movie;
        this.rated = rated;
    }

    public MovieRating(Movie movie, boolean rated, boolean awkward) {
        this.movie = movie;
        this.rated = rated;
        this.awkward = awkward;
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String id) {
        this.movieId = id;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
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
