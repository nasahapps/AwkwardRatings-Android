package com.nasahapps.awkwardratings;

import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.Scanner;

/**
 * Created by Hakeem on 3/8/15.
 */
public class AnalyticsHelper {

    private static AnalyticsHelper sInstance;
    private Context mContext;
    private Tracker mTracker;

    private AnalyticsHelper(Context c) {
        mContext = c.getApplicationContext();
        Scanner s = new Scanner(mContext.getResources().openRawResource(R.raw.ganalytics));
        try {
            while (s.hasNext()) {
                mTracker = GoogleAnalytics.getInstance(mContext).newTracker(s.next());
                mTracker.enableAdvertisingIdCollection(true);
                mTracker.enableAutoActivityTracking(true);
            }
        } finally {
            s.close();
        }
    }

    public static AnalyticsHelper getInstance(Context c) {
        if (sInstance == null)
            sInstance = new AnalyticsHelper(c);
        return sInstance;
    }

    public void sendScreenViewAnalytics(String path) {
        if (mTracker != null) {
            mTracker.setScreenName(path);
            mTracker.send(new HitBuilders.AppViewBuilder().build());
        }
    }

}
