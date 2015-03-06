package com.nasahapps.awkwardratings;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;

/**
 * Created by Hakeem on 2/28/15.
 * <p/>
 * Just convenience methods I use in all my apps, gathered into one collective helper class.
 */
public class Utils {

    /**
     * Used to clear the Activity stack and so the next called Activity
     * will be at the very bottom of the stack, and pressing "back" will end the app
     * instead of going to the previous Activity
     *
     * @param i Intent to start
     * @return the same Intent
     */
    public static Intent clearActivityStack(Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    /**
     * Log an error as well as display it in a Toast
     * @param c Context
     * @param tag tag for logging
     * @param log log message
     * @param toast message for the Toast
     */
    public static void showError(Context c, String tag, String log, String toast) {
        showError(c, tag, log, null, toast);
    }

    /**
     * Log an error as well as display it in a Toast
     * @param c Context
     * @param tag tag for logging
     * @param log log message
     * @param e Exception to log
     * @param toast message for the Toast
     */
    public static void showError(Context c, String tag, String log, Exception e, String toast) {
        if (e != null)
            Log.e(tag, log, e);
        else
            Log.e(tag, log);
        Toast.makeText(c, toast, Toast.LENGTH_SHORT).show();
    }

    /**
     * Used to color any Drawable to the app's primary color, such as changing the color of an icon to
     * fit in better with the app's color scheme. Works best on white Drawables.
     * @param c Context
     * @param res drawable resource
     * @return the colored Drawable
     */
    public static Drawable getColoredDrawable(Context c, int res) {
        Drawable d = c.getResources().getDrawable(res);
        TypedValue typedValue = new TypedValue();
        c.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
        int color = typedValue.data;
        d.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        return d;
    }

    /**
     * Convert DP units to pixels
     * @param res Resources
     * @param dp DP to convert
     * @return pixels
     */
    public static float dpToPixel(Resources res, int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
    }

    /**
     * Check if the API level of the current device is >= the given API level
     * @param apiLevel API level to compare to
     * @return true if >= the given API level, false if under
     */
    public static boolean isAtApiLevel(int apiLevel) {
        return Build.VERSION.SDK_INT >= apiLevel;
    }

    /**
     * Quick fix to RecyclerView not working correctly with SwipeRefreshLayout when swiping down while
     * in the middle of a list.
     * @param view RecyclerView to fix
     * @param refreshLayout SwipeRefreshLayout parent of the RecyclerView
     */
    public static void fixSwipeBug(RecyclerView view, final SwipeRefreshLayout refreshLayout) {
        view.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView view, int dx, int dy) {
                boolean enable = true;
                if (view != null && view.getChildCount() > 0) {
                    // check if the first item of the list is visible
                    boolean firstItemVisible = ((LinearLayoutManager) view.getLayoutManager())
                            .findFirstVisibleItemPosition() == 0;
                    // check if the top of the first item is visible
                    boolean topOfFirstItemVisible = view.getChildAt(0).getTop() == 0;
                    // enabling or disabling the refresh layout
                    enable = firstItemVisible && topOfFirstItemVisible;
                }
                refreshLayout.setEnabled(enable);
            }
        });
    }

    /**
     * Checks if the device is at least a 7-in tablet
     * @param c Context
     * @return true if a tablet, false otherwise
     */
    public static boolean isTablet(Context c) {
        return c.getResources().getBoolean(R.bool.isTablet);
    }

    /**
     * Checks if the device is a 10-in tablet
     * @param c Context
     * @return true if 10 inches (like Nexus 10), false if under
     */
    public static boolean is10Inches(Context c) {
        int dp = c.getResources().getConfiguration().smallestScreenWidthDp;
        return dp >= 720;
    }

    /**
     * Checks if the device is currently in portrait mode
     * @param c Context
     * @return true if in portrait, false if landscape
     */
    public static boolean isPortrait(Context c) {
        return c.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    /**
     * Quickly concat a list of Strings separated by commas
     * @param strings list of Strings to concat
     * @return the list of Strings concatenated and separated by commas
     */
    public static String buildString(List<String> strings) {
        if (strings == null) return null;
        if (strings.isEmpty()) return "";
        if (strings.size() == 1) return strings.get(0);

        StringBuilder sb = new StringBuilder();
        for (String s : strings) {
            sb.append(s + ", ");
        }
        sb.replace(sb.length() - 2, sb.length(), "");
        return sb.toString();
    }

    /**
     * Easy method to create a loading ProgressDialog
     * @param c Context
     * @param message String you want displayed (e.g. "Loading...")
     * @return a ProgressDialog. To start is, just call ProgressDialog.show().
     */
    public static ProgressDialog createProgressDialog(Context c, String message) {
        ProgressDialog dialog = new ProgressDialog(c, ProgressDialog.STYLE_SPINNER);
        dialog.setIndeterminate(true);
        dialog.setMessage(message);
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    /**
     * Checks if the user has a network connection
     * @param c Context
     * @return true if valid network connection
     */
    public static boolean hasInternet(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            Toast.makeText(c, "No internet connection", Toast.LENGTH_SHORT).show();
        }
        return isConnected;
    }

    /**
     * Gets the Bitmap of a ImageView, mainly used for later getting a Palette color
     * but could also be used for whatever you need an ImageView's Bitmap for
     * @param iv ImageView
     * @return the Bitmap of the ImageView
     */
    public static Bitmap getImageViewBitmap(ImageView iv) {
        if (iv.getDrawable() != null) {
            return ((BitmapDrawable) iv.getDrawable()).getBitmap();
        } else return null;
    }

    /**
     * Method to smoothly animate from the view's current bg color to a new color
     * @param v View
     * @param color new color to animate to (hexadecimal)
     */
    public static void animateToColor(View v, int color) {
        int originalColor = v.getBackground() != null ? ((ColorDrawable) v.getBackground()).getColor()
                : 0xff000000;
        ValueAnimator colorAnim = ObjectAnimator.ofInt(v, "backgroundColor",
                originalColor, color);
        colorAnim.setDuration(250);
        colorAnim.setEvaluator(new ArgbEvaluator());
        colorAnim.setInterpolator(new DecelerateInterpolator());
        colorAnim.start();
    }

    /**
     * Method to smoothly animate from black to a new color
     * @param v
     * @param color
     */
    public static void animateFromBlackToColor(View v, int color) {
        ValueAnimator colorAnim = ObjectAnimator.ofInt(v, "backgroundColor",
                0xff000000, color);
        colorAnim.setDuration(250);
        colorAnim.setEvaluator(new ArgbEvaluator());
        colorAnim.setInterpolator(new DecelerateInterpolator());
        colorAnim.start();
    }

    /**
     * Checks if user has an app installed to handle the given Intent
     * @param i Intent to start
     * @param c Context
     * @return true if user has an app to handle the Intent, false otherwise
     */
    public static boolean hasValidAppToOpen(Intent i, Context c) {
        return i.resolveActivity(c.getPackageManager()) != null;
    }

    /**
     * Programmatically lock the screen orientation
     * @param a Activity
     */
    public static void setScreenOrientation(Activity a) {
        if (!isTablet(a)) {
            a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    /**
     * @param a Activity
     * @return a Point, where the width is Point.x and height is Point.y
     */
    public static Point getScreenDimensions(Activity a) {
        Display d = a.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        d.getSize(size);
        return size;
    }

    /**
     * @param a Activity
     * @return action bar height in pixels
     */
    public static int getActionBarHeight(Activity a) {
        TypedValue tv = new TypedValue();
        if (a.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, a.getResources().getDisplayMetrics());
        } else return 0;
    }

    /**
     * @param c Context (such as an Activity)
     * @return status bar height in pixels
     */
    public static int getStatusBarHeight(Context c) {
        int result = 0;
        int resourceId = c.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = c.getResources().getDimensionPixelSize(resourceId);
        }

        return result;
    }

    /**
     * Show a Dialog asking the user to rate the app
     * This implementation asks after being called every 10 times
     *
     * @param c           Context
     * @param rateCounter a counter used to keep track of how many times this function was called
     */
    public static void showRateDialog(final Context c, int rateCounter) {
        if (!PreferencesHelper.getInstance(c).getBoolean(PreferencesHelper.KEY_RATE_APP, false)
                && rateCounter % 10 == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(c);
            builder.setTitle("Rate the app")
                    .setMessage(String.format("Like %1$s? Rate it in the Play Store!", c.getString(R.string.app_name)))
                    .setPositiveButton("Rate", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PreferencesHelper.getInstance(c).putBoolean(PreferencesHelper.KEY_RATE_APP, true);

                            Uri link = Uri.parse("market://details?id=" + c.getPackageName());
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(link);
                            c.startActivity(i);
                        }
                    })
                    .setNegativeButton("No thanks", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
            builder.create().show();
        }
    }

}
