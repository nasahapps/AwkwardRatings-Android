package com.nasahapps.awkwardratings.ui;

import android.app.Application;

import com.firebase.client.Firebase;
import com.nasahapps.awkwardratings.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by Hakeem on 2/28/15.
 */
public class AwkwardRatingsApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Uses Parse key and secret
        Scanner s = new Scanner(getResources().openRawResource(R.raw.parse));
        try {
            List<String> parseKeys = new ArrayList<>();
            while (s.hasNext()) {
                parseKeys.add(s.next());
            }
            Firebase.setAndroidContext(this);
        } finally {
            s.close();
        }
    }
}
