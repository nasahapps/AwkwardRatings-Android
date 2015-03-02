package com.nasahapps.awkwardratings;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;

/**
 * Created by Hakeem on 2/28/15.
 *
 * Just convenience methods I use in all my apps, gathered into one collective helper class.
 */
public class Utils {

    public static Intent clearActivityStack(Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    public static void showError(Context c, String tag, String log, String toast) {
        showError(c, tag, log, null, toast);
    }

    public static void showError(Context c, String tag, String log, Exception e, String toast) {
        if (e != null)
            Log.e(tag, log, e);
        else
            Log.e(tag, log);
        Toast.makeText(c, toast, Toast.LENGTH_SHORT).show();
    }

    public static Drawable getColoredDrawable(Context c, int res) {
        Drawable d = c.getResources().getDrawable(res);
        TypedValue typedValue = new TypedValue();
        c.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
        int color = typedValue.data;
        d.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        return d;
    }

    public static float dpToPixel(Resources res, int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
    }

    public static boolean isAtApiLevel(int apiLevel) {
        return Build.VERSION.SDK_INT >= apiLevel;
    }

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

    public static boolean isTablet(Context c) {
        return c.getResources().getBoolean(R.bool.isTablet);
    }

    public static boolean is10Inches(Context c) {
        int dp = c.getResources().getConfiguration().smallestScreenWidthDp;
        return dp >= 720;
    }

    public static boolean isPortrait(Context c) {
        return c.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

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

    public static ProgressDialog createProgressDialog(Context c, String message) {
        ProgressDialog dialog = new ProgressDialog(c, ProgressDialog.STYLE_SPINNER);
        dialog.setIndeterminate(true);
        dialog.setMessage(message);
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    public static boolean hasInternet(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            Toast.makeText(c, "No internet connection", Toast.LENGTH_SHORT).show();
        }
        return isConnected;
    }

    public static Bitmap getImageViewBitmap(ImageView iv) {
        if (iv.getDrawable() != null) {
            return ((BitmapDrawable) iv.getDrawable()).getBitmap();
        } else return null;
    }

    public static void animateToColor(View v, int color) {
        ValueAnimator colorAnim = ObjectAnimator.ofInt(v, "backgroundColor",
                ((ColorDrawable) v.getBackground()).getColor(), color);
        colorAnim.setDuration(250);
        colorAnim.setEvaluator(new ArgbEvaluator());
        colorAnim.setInterpolator(new DecelerateInterpolator());
        colorAnim.start();
    }

}
