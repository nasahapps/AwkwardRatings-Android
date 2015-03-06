package com.nasahapps.awkwardratings.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.nasahapps.awkwardratings.PreferencesHelper;
import com.nasahapps.awkwardratings.Utils;

/**
 * Created by Hakeem on 3/5/15.
 * <p/>
 * This Activity is to determine if the user has already opened the app before
 * and if we should show them the IntroActivity first.
 */
public class StartActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Do a check on startup if user has opened the app before
        // If they haven't, show the welcome screen
        Intent i;
        if (PreferencesHelper.getInstance(this).getBoolean(PreferencesHelper.KEY_OPENED_ALREADY, false)) {
            i = new Intent(this, MainActivity.class);
        } else {
            i = new Intent(this, IntroActivity.class);
            PreferencesHelper.getInstance(this).putBoolean(PreferencesHelper.KEY_OPENED_ALREADY, true);
        }
        // This is to make sure when the user presses the back button that it doesn't go
        // back to this Activity
        // In other words, the next Activity is at the bottom of the stack of Activities for this app
        Utils.clearActivityStack(i);
        startActivity(i);
    }

}
