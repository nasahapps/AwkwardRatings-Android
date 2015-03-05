package com.nasahapps.awkwardratings.service;

import android.content.Context;
import android.util.Log;

import com.nasahapps.awkwardratings.PreferencesHelper;
import com.nasahapps.awkwardratings.model.MovieRating;
import com.parse.ParseObject;

import java.util.Map;

/**
 * Created by Hakeem on 3/4/15.
 */
public class VoteHelper {

    private static final String TAG = VoteHelper.class.getSimpleName();

    public static void vote(Context c, ParseObject movie, boolean awkward, Map<Integer, MovieRating> movieRatings) {
        String voteKey = awkward ? "awkward_yes" : "awkward_no";

        if (movieRatings.containsKey(movie.getNumber("movie_id").intValue())) {
            // User has voted on this movie before
            // First check if user pressed the same vote button
            // If so, unvote
            MovieRating movieRating = movieRatings.get(movie.getNumber("movie_id").intValue());
            try {
                if (awkward == movieRating.isAwkward()) {
                    // e.g. user voted yes before, now is unvoting yes
                    // First decrement a vote for this movie
                    if (movie.fetchIfNeeded().getNumber(voteKey) != null) {
                        movie.put(voteKey, movie.getNumber(voteKey).longValue() - 1);
                    }
                    movie.saveInBackground();

                    // Then remove this rating from the user's prefs
                    movieRatings.remove(movie.getNumber("movie_id").intValue());
                    PreferencesHelper.getInstance(c).saveMovieRatings(movieRatings);
                } else {
                    // e.g. user voted yes before, now is voting no
                    // First decrement the original vote for this movie
                    String otherKey = awkward ? "awkward_no" : "awkward_yes";
                    if (movie.fetchIfNeeded().getNumber(otherKey) != null) {
                        movie.put(otherKey, movie.getNumber(otherKey).longValue() - 1);
                    }
                    // Then increment the new vote
                    if (movie.getNumber(voteKey) != null) {
                        movie.increment(voteKey);
                    } else {
                        movie.put(voteKey, 1);
                    }
                    movie.saveInBackground();

                    // Then change the rating to the user
                    movieRating.setAwkward(awkward);
                    movieRatings.put(movie.getNumber("movie_id").intValue(), movieRating);
                    PreferencesHelper.getInstance(c).saveMovieRatings(movieRatings);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching MovieRating or Movie object", e);
            }
        } else {
            // User hasn't voted on this movie before
            // So first increment a vote for this movie
            try {
                if (movie.fetchIfNeeded().getNumber(voteKey) != null) {
                    movie.increment(voteKey);
                } else {
                    movie.put(voteKey, 1);
                }
                movie.saveInBackground();

                // Then save this rating to the user
                // Make the MovieRating object
                MovieRating movieRating = new MovieRating(movie.getNumber("movie_id"), awkward);

                // Then add it to the user's map of movie ratings
                movieRatings.put(movie.getNumber("movie_id").intValue(), movieRating);
                PreferencesHelper.getInstance(c).saveMovieRatings(movieRatings);
            } catch (Exception e) {
                Log.e(TAG, "Error fetching Movie object", e);
            }
        }
    }

    public static long getVote(ParseObject movie) {
        Number yes = movie.getNumber("awkward_yes");
        Number no = movie.getNumber("awkward_no");
        if (yes.longValue() == 0 && no.longValue() == 0) {
            return -1;
        } else {
            return yes.longValue() * 100 / (yes.longValue() + no.longValue());
        }
    }

}
