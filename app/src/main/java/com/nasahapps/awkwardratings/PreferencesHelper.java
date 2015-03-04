package com.nasahapps.awkwardratings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.nasahapps.awkwardratings.model.MovieRating;

import java.util.Map;

/**
 * Created by Hakeem on 1/10/15.
 */
public class PreferencesHelper {

    public static final String KEY_RATE_APP = "rate_app";
    public static final String KEY_RATE_APP_COUNTER = "rate_app_counter";
    public static final String KEY_MOVIE_RATINGS = "movie_ratings";

    private static PreferencesHelper sInstance;
    private SharedPreferences mPrefs;
    private Context mContext;

    private PreferencesHelper(Context c) {
        mContext = c.getApplicationContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public static PreferencesHelper getInstance(Context c) {
        if (sInstance == null)
            sInstance = new PreferencesHelper(c);
        return sInstance;
    }

    public void putInt(String key, int value) {
        mPrefs.edit().putInt(key, value).apply();
    }

    public void putBoolean(String key, boolean value) {
        mPrefs.edit().putBoolean(key, value).apply();
    }

    public void putString(String key, String value) {
        mPrefs.edit().putString(key, value).apply();
    }

    public int getInt(String key, int def) {
        return mPrefs.getInt(key, def);
    }

    public boolean getBoolean(String key, boolean def) {
        return mPrefs.getBoolean(key, def);
    }

    public String getString(String key, String def) {
        return mPrefs.getString(key, def);
    }

    /**
     * App-specific save/load methods
     */

    public void saveMovieRatings(Map<Number, MovieRating> ratings) {
        String json = new Gson().toJson(ratings);
        mPrefs.edit().putString(KEY_MOVIE_RATINGS, json).apply();
    }

}
