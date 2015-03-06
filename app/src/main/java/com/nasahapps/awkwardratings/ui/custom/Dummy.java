package com.nasahapps.awkwardratings.ui.custom;

import com.nasahapps.awkwardratings.model.Movie;
import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hakeem on 3/5/15.
 * <p/>
 * Used for creating dummy data for screenshots
 * else, Google's blind bot will take this app down
 */
public class Dummy {

    private List<ParseObject> mParseMovies;
    private Movie mMovie;

    public Dummy() {
        mParseMovies = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            ParseObject movie = new ParseObject("Movie");
            movie.put("movie_id", i);
            movie.put("title", "Movie #" + i);
            movie.put("awkward_yes", i);
            movie.put("awkward_no", i * (i + 1));
            mParseMovies.add(movie);
        }

        mMovie = new Movie();

    }

    public List<ParseObject> getParseMovies() {
        return mParseMovies;
    }
}
