package com.nasahapps.awkwardratings.ui;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.TextView;

import com.nasahapps.awkwardratings.R;
import com.nasahapps.awkwardratings.Utils;

/**
 * Created by Hakeem on 3/5/2015
 * <p/>
 * Intro Activity that tells the user what this app is about
 */
public class IntroActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);
        // Let's have the IntroActivity be portrait-only
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new IntroFragment())
                    .commit();
        }
    }

    public static class IntroFragment extends Fragment {

        private Button mStartButton;
        private TextView text1, text2, text3, text4, text5, text6, text7, text8, text9, text10;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_intro, container, false);
            setRetainInstance(true);

            text1 = (TextView) v.findViewById(R.id.text1);
            text2 = (TextView) v.findViewById(R.id.text2);
            text3 = (TextView) v.findViewById(R.id.text3);
            text4 = (TextView) v.findViewById(R.id.text4);
            text5 = (TextView) v.findViewById(R.id.text5);
            text6 = (TextView) v.findViewById(R.id.text6);
            text7 = (TextView) v.findViewById(R.id.text7);
            text8 = (TextView) v.findViewById(R.id.text8);
            text9 = (TextView) v.findViewById(R.id.text9);
            text10 = (TextView) v.findViewById(R.id.text10);

            mStartButton = (Button) v.findViewById(R.id.startButton);
            mStartButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Start the app properly
                    Intent i = new Intent(getActivity(), MainActivity.class);
                    Utils.clearActivityStack(i);
                    startActivity(i);
                }
            });

            // Animate our views in
            fadeAndSlideIn(text1, false, 0);
            fadeIn(text2, 1500);
            fadeAndSlideIn(text3, false, 3500);
            fadeAndSlideIn(text4, true, 5000);
            fadeAndSlideIn(text5, false, 6500);
            fadeAndSlideIn(text6, true, 8000);
            fadeAndSlideIn(text7, false, 9500);
            fadeAndSlideIn(text8, true, 11000);
            fadeAndSlideIn(text9, false, 14000);
            fadeAndSlideIn(text10, true, 15000);
            fadeAndSlideIn(mStartButton, true, 15500);

            return v;
        }

        public void fadeIn(View v, long delay) {
            if (v.getVisibility() != View.VISIBLE) {
                v.setVisibility(View.VISIBLE);
                AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
                alpha.setDuration(1000);
                alpha.setStartOffset(delay);
                alpha.setInterpolator(new DecelerateInterpolator());
                v.startAnimation(alpha);
            }
        }

        public void fadeAndSlideIn(View v, boolean toLeft, long delay) {
            if (v.getVisibility() != View.VISIBLE) {
                v.setVisibility(View.VISIBLE);
                AnimationSet as = new AnimationSet(true);
                AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
                TranslateAnimation trans = new TranslateAnimation(toLeft ? 100 : -100, 0, 0f, 0f);

                as.addAnimation(alpha);
                as.addAnimation(trans);
                as.setDuration(1000);
                as.setStartOffset(delay);
                as.setInterpolator(new DecelerateInterpolator());
                v.startAnimation(as);
            }
        }
    }
}
