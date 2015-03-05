package com.nasahapps.awkwardratings.service;

import android.content.Context;
import android.util.Log;

import com.nasahapps.awkwardratings.BuildConfig;
import com.nasahapps.awkwardratings.R;
import com.nasahapps.awkwardratings.model.Movie;
import com.nasahapps.awkwardratings.service.response.MovieResponse;

import java.util.Scanner;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by Hakeem on 2/28/15.
 *
 * Network helper class for any REST APIs used
 */
public class NetworkHelper {

    private static final String TAG = NetworkHelper.class.getSimpleName();

    private static final String MOVIE_DB_ENDPOINT = "https://api.themoviedb.org/3";
    private static final String MOVIES_POPULAR = "/movie/popular";
    private static final String MOVIE_SPECIFIC = "/movie/{id}";
    private static final String SEARCH_MOVIE = "/search/movie";

    private static NetworkHelper sInstance;

    private String mTMDBKey;
    private MovieDatabaseClient mMovieDBClient;

    private NetworkHelper(Context c) {
        Scanner s = new Scanner(c.getResources().openRawResource(R.raw.tmdb));
        try {
            // While statement should only run once, there's only one line in the file
            while (s.hasNext()) {
                mTMDBKey = s.next();
            }
        } finally {
            s.close();
        }

        // If the key was null, you should get yourself a key!
        // Else this app won't work. Can't help you there!
        // https://www.themoviedb.org/documentation/api
        if (mTMDBKey != null) {
            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setLogLevel(BuildConfig.DEBUG ? RestAdapter.LogLevel.FULL
                            : RestAdapter.LogLevel.NONE)
                    .setEndpoint(MOVIE_DB_ENDPOINT)
                    .build();
            mMovieDBClient = restAdapter.create(MovieDatabaseClient.class);
        }
    }

    public static NetworkHelper getInstance(Context c) {
        if (sInstance == null)
            sInstance = new NetworkHelper(c);
        return sInstance;
    }

    public void getPopularMovies(int page) {
        mMovieDBClient.getPopularMovies(mTMDBKey, page, new Callback<MovieResponse>() {
            @Override
            public void success(MovieResponse movieResponse, Response response) {
                EventBus.getDefault().post(movieResponse);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Error getting popular movies: " + error.getLocalizedMessage());
            }
        });
    }

    public void getMovie(int id) {
        mMovieDBClient.getMovie(id, mTMDBKey, "videos", new Callback<Movie>() {
            @Override
            public void success(Movie movie, Response response) {
                EventBus.getDefault().post(movie);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Error getting movie: " + error.getLocalizedMessage());
                EventBus.getDefault().post(error);
            }
        });
    }

    public void searchMovie(String query) {
        mMovieDBClient.searchMovie(mTMDBKey, query, new Callback<MovieResponse>() {
            @Override
            public void success(MovieResponse movieResponse, Response response) {
                EventBus.getDefault().post(movieResponse);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Error searching movies: " + error.getLocalizedMessage());
                EventBus.getDefault().post(error);
            }
        });
    }

    public String getApiKey() {
        return mTMDBKey;
    }

    interface MovieDatabaseClient {
        // Getting the day's popular movies
        @GET(MOVIES_POPULAR)
        void getPopularMovies(
                @Query("api_key") String key,
                @Query("page") int page,
                Callback<MovieResponse> cb
        );

        // Getting info on a specific movie
        @GET(MOVIE_SPECIFIC)
        void getMovie(
                @Path("id") int id,
                @Query("api_key") String key,
                @Query("append_to_response") String append,
                Callback<Movie> cb
        );

        // For searching movies
        @GET(SEARCH_MOVIE)
        void searchMovie(
                @Query("api_key") String key,
                @Query("query") String query,
                Callback<MovieResponse> cb
        );
    }
}
