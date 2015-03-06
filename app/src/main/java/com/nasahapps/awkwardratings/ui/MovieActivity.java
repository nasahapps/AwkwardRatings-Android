package com.nasahapps.awkwardratings.ui;

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.transition.ChangeImageTransform;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.nasahapps.awkwardratings.PreferencesHelper;
import com.nasahapps.awkwardratings.R;
import com.nasahapps.awkwardratings.Utils;
import com.nasahapps.awkwardratings.model.Movie;
import com.nasahapps.awkwardratings.model.MovieRating;
import com.nasahapps.awkwardratings.service.NetworkHelper;
import com.nasahapps.awkwardratings.service.VoteHelper;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Scanner;

import de.greenrobot.event.EventBus;
import retrofit.RetrofitError;

public class MovieActivity extends ActionBarActivity {

    public static final String EXTRA_ID = "id";
    private static final String TAG = MovieActivity.class.getSimpleName();

    @TargetApi(21)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Utils.isAtApiLevel(21)) {
            // Set window transitions for when user clicks a movie
            getWindow().setAllowEnterTransitionOverlap(true);
            getWindow().setAllowReturnTransitionOverlap(true);
            getWindow().setSharedElementExitTransition(new ChangeImageTransform());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);
        if (savedInstanceState == null) {
            int id = getIntent().getExtras().getInt(EXTRA_ID);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, MovieFragment.newInstance(id))
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    public static class MovieFragment extends Fragment {

        private View mBackground, mSeparatorOne, mAwkwardLabel;
        private ImageView mBackdrop, mPoster;
        private TextView mTitle, mMoreInfo, mOverview;
        private Button mNotAwkwardButton, mAwkwardButton, mAwkwardness;
        private ImageButton mPlayTrailer;
        private Map<Integer, MovieRating> mMovieRatings;
        private int mId;
        private Toolbar mToolbar;
        private Movie mMovie;

        public static MovieFragment newInstance(int id) {
            Bundle args = new Bundle();
            args.putInt(EXTRA_ID, id);

            MovieFragment fragment = new MovieFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_movie, container, false);
            setRetainInstance(true);
            mId = getArguments().getInt(EXTRA_ID);
            mMovieRatings = PreferencesHelper.getInstance(getActivity()).loadMovieRatings();

            mToolbar = (Toolbar) v.findViewById(R.id.toolbar);
            mToolbar.setTitle("");
            ((ActionBarActivity) getActivity()).setSupportActionBar(mToolbar);
            ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            // Initialize views
            // mBackground is used for adjusting the background color of the entire view
            mBackground = v.findViewById(R.id.parentLayout);
            // mAwkwardness is the view that shows how awkward this movie is
            mAwkwardness = (Button) v.findViewById(R.id.awkwardness);
            mBackdrop = (ImageView) v.findViewById(R.id.backdrop);
            mPoster = (ImageView) v.findViewById(R.id.poster);
            mTitle = (TextView) v.findViewById(R.id.title);
            mMoreInfo = (TextView) v.findViewById(R.id.moreInfo);
            mOverview = (TextView) v.findViewById(R.id.overview);
            mNotAwkwardButton = (Button) v.findViewById(R.id.notAwkwardButton);
            mAwkwardButton = (Button) v.findViewById(R.id.awkwardButton);
            mPlayTrailer = (ImageButton) v.findViewById(R.id.playTrailer);
            mSeparatorOne = v.findViewById(R.id.separatorOne);
            mAwkwardLabel = v.findViewById(R.id.awkwardLabel);

            EventBus.getDefault().register(this);

            if (savedInstanceState == null) {
                getMovie();
            } else {
                mNotAwkwardButton.setVisibility(View.VISIBLE);
                mAwkwardness.setVisibility(View.VISIBLE);
                mAwkwardButton.setVisibility(View.VISIBLE);
                mTitle.setVisibility(View.VISIBLE);
                mMoreInfo.setVisibility(View.VISIBLE);
                mOverview.setVisibility(View.VISIBLE);
                mSeparatorOne.setVisibility(View.VISIBLE);
                mPoster.setVisibility(View.VISIBLE);
                if (Utils.isPortrait(getActivity())) {
                    // Only set the awkward label to visible if in portrait
                    mAwkwardLabel.setVisibility(View.VISIBLE);
                }
                onEvent(mMovie);
            }

            return v;
        }

        @Override
        public void onStart() {
            super.onStart();
            if (!EventBus.getDefault().isRegistered(this))
                EventBus.getDefault().register(this);
        }

        @Override
        public void onStop() {
            super.onStop();
            if (EventBus.getDefault().isRegistered(this))
                EventBus.getDefault().unregister(this);
        }

        public void getMovie() {
            NetworkHelper.getInstance(getActivity()).getMovie(mId);
        }

        public void onEvent(Movie movie) {
            if (movie != null) {
                mMovie = movie;
                // Set the backdrop and poster, if available
                if (mMovie.getBackdropPath() != null) {
                    Uri uri = Uri.parse("https://image.tmdb.org/t/p/w" + (Utils.isTablet(getActivity())
                            ? (Utils.is10Inches(getActivity()) ? "1000" : "500") : "300") + mMovie.getBackdropPath()
                            + "?api_key=" + NetworkHelper.getInstance(getActivity()).getApiKey());
                    Picasso.with(getActivity()).load(uri).into(mBackdrop);
                } else {
                    // Set the backdrop height to 48dp
                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            (int) Utils.dpToPixel(getResources(), 96));
                    mBackdrop.setLayoutParams(lp);
                }
                if (mMovie.getPosterPath() != null) {
                    Uri uri = Uri.parse("https://image.tmdb.org/t/p/w" + (Utils.isTablet(getActivity())
                            ? "300" : "150") + mMovie.getPosterPath()
                            + "?api_key=" + NetworkHelper.getInstance(getActivity()).getApiKey());
                    Picasso.with(getActivity()).load(uri).into(mPoster, new Callback() {
                        @Override
                        public void onSuccess() {
                            mPoster.setOnClickListener(new View.OnClickListener() {
                                @TargetApi(21)
                                @Override
                                public void onClick(View v) {
                                    Intent i = new Intent(getActivity(), PosterActivity.class);
                                    i.putExtra(PosterActivity.EXTRA_URL, mMovie.getPosterPath());
                                    if (Utils.isAtApiLevel(21)) {
                                        // Blow the image up to size
                                        getActivity().startActivity(i, ActivityOptions
                                                .makeSceneTransitionAnimation(getActivity(), mPoster, "poster").toBundle());
                                    } else {
                                        startActivity(i);
                                    }
                                }
                            });
                            Palette.generateAsync(Utils.getImageViewBitmap(mPoster), new Palette.PaletteAsyncListener() {
                                @Override
                                public void onGenerated(Palette p) {
                                    int color = p.getDarkMutedColor(p.getMutedColor(p.getDarkVibrantColor(0xff000000)));
                                    Utils.animateFromBlackToColor(mBackground, color);
                                }
                            });
                        }

                        @Override
                        public void onError() {

                        }
                    });
                } else {
                    mPoster.setVisibility(View.GONE);
                }

                // Hide the play button if there is no trailer
                // Else set up the click listener
                if (mMovie.getVideos() != null
                        && mMovie.getVideos().getResults() != null
                        && !mMovie.getVideos().getResults().isEmpty()
                        && mMovie.getVideos().getResults().get(0).getSite().equals("YouTube")
                        && mMovie.getVideos().getResults().get(0).getType().equals("Trailer")) {
                    mPlayTrailer.setVisibility(View.VISIBLE);
                    mPlayTrailer.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // You're gonna need a YouTube API key to view YT vids, just so you know
                            // https://developers.google.com/youtube/android/player/
                            Scanner s = new Scanner(getResources().openRawResource(R.raw.youtube));
                            try {
                                String key = null;
                                while (s.hasNext()) {
                                    key = s.next();
                                }
                                Intent i = YouTubeStandalonePlayer.createVideoIntent(getActivity(), key,
                                        mMovie.getVideos().getResults().get(0).getKey(), 0, true, false);
                                // YouTube app must be installed, so check for that
                                if (Utils.hasValidAppToOpen(i, getActivity()))
                                    startActivity(i);
                                else
                                    Toast.makeText(getActivity(), "YouTube app not installed", Toast.LENGTH_SHORT)
                                            .show();
                            } finally {
                                s.close();
                            }
                        }
                    });
                }

                // Set the movie title
                mTitle.setText(mMovie.getTitle());
                // Additional movie info will be "Month yyyy <dot> mmm minutes"
                try {
                    if (mMovie.getReleaseDate() != null) {
                        SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd");
                        Date date = originalFormat.parse(mMovie.getReleaseDate());
                        SimpleDateFormat newFormat = new SimpleDateFormat("MMMM yyyy");
                        String formattedDate = newFormat.format(date);
                        StringBuilder sb = new StringBuilder(formattedDate);
                        // If we have runtime length, add it to the string
                        if (mMovie.getRuntime() != 0) {
                            sb.append(" \u00b7 " + mMovie.getRuntime() + " minutes");
                        }
                        mMoreInfo.setText(sb.toString());
                    } else {
                        if (mMovie.getRuntime() != 0) {
                            mMoreInfo.setText(mMovie.getRuntime() + " minutes");
                        } else {
                            mMoreInfo.setVisibility(View.GONE);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing date", e);
                    if (mMovie.getRuntime() != 0) {
                        mMoreInfo.setText(mMovie.getRuntime() + " minutes");
                    } else {
                        mMoreInfo.setVisibility(View.GONE);
                    }
                }

                // Highlight the proper awkward buttons if user has already voted on this movie
                ParseQuery<ParseObject> query = ParseQuery.getQuery("Movie");
                query.whereEqualTo("movie_id", mId);
                query.getFirstInBackground(new GetCallback<ParseObject>() {
                    @Override
                    public void done(final ParseObject parseObject, ParseException e) {
                        if (e == null) {
                            boolean hasRated = mMovieRatings.containsKey(parseObject.getNumber("movie_id").intValue());
                            if (hasRated) {
                                // Highlight yes button red if voted yes, no button green if voted no
                                MovieRating movieRating = mMovieRatings.get(parseObject.getNumber("movie_id").intValue());
                                if (movieRating.isAwkward()) {
                                    mAwkwardButton.setBackgroundResource(R.drawable.red_circle);
                                } else {
                                    mNotAwkwardButton.setBackgroundResource(R.drawable.green_circle);
                                }
                            }

                            long percent = VoteHelper.getVote(parseObject);
                            if (percent == -1)
                                mAwkwardness.setText("NR");
                            else
                                mAwkwardness.setText(percent + "%");

                            // Set button clicks
                            mAwkwardButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (mMovieRatings.containsKey(mId)) {
                                        boolean awkward = mMovieRatings.get(mId).isAwkward();
                                        if (awkward) {
                                            // User originally voted yes, now is removing that vote
                                            mAwkwardButton.setBackgroundResource(R.drawable.transparent_circle);
                                        } else {
                                            // User originally voted no, switching vote from no to yes
                                            mNotAwkwardButton.setBackgroundResource(R.drawable.transparent_circle);
                                            mAwkwardButton.setBackgroundResource(R.drawable.red_circle);
                                        }
                                    } else {
                                        // New vote
                                        mAwkwardButton.setBackgroundResource(R.drawable.red_circle);
                                    }
                                    VoteHelper.vote(getActivity(), parseObject, true, mMovieRatings);
                                    // And adjust the middle button's percentage
                                    long percent = VoteHelper.getVote(parseObject);
                                    if (percent == -1)
                                        mAwkwardness.setText("NR");
                                    else
                                        mAwkwardness.setText(percent + "%");
                                }
                            });
                            mNotAwkwardButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (mMovieRatings.containsKey(mId)) {
                                        boolean awkward = mMovieRatings.get(mId).isAwkward();
                                        if (!awkward) {
                                            // User originally voted no, now is removing that vote
                                            mNotAwkwardButton.setBackgroundResource(R.drawable.transparent_circle);
                                        } else {
                                            // User originally voted yes, switching vote from yes to no
                                            mAwkwardButton.setBackgroundResource(R.drawable.transparent_circle);
                                            mNotAwkwardButton.setBackgroundResource(R.drawable.green_circle);
                                        }
                                    } else {
                                        // New vote
                                        mNotAwkwardButton.setBackgroundResource(R.drawable.green_circle);
                                    }
                                    VoteHelper.vote(getActivity(), parseObject, false, mMovieRatings);
                                    // And adjust the middle button's percentage
                                    long percent = VoteHelper.getVote(parseObject);
                                    if (percent == -1)
                                        mAwkwardness.setText("NR");
                                    else
                                        mAwkwardness.setText(percent + "%");
                                }
                            });

                            // Fade the poster in
                            fadeAndSlideDownIn(mPoster, 0);
                            // Pop the buttons in
                            popIn(mNotAwkwardButton, 0);
                            popIn(mAwkwardness, 100);
                            if (Utils.isPortrait(getActivity()))
                                // Only fade in the awkward label if in portrait
                                fadeIn(mAwkwardLabel, 100);
                            popIn(mAwkwardButton, 200);
                            // And fade the text in
                            fadeIn(mTitle, 0);
                            fadeIn(mMoreInfo, 100);
                            // And "draw" the line across
                            lineAcross(mSeparatorOne, 100);
                            fadeIn(mOverview, 200);
                        } else {
                            Utils.showError(getActivity(), TAG, "Error querying movie", e, e.getLocalizedMessage());
                        }
                    }
                });

                if (mMovie.getOverview() != null) {
                    mOverview.setText(mMovie.getOverview());
                }
            } else {
                Toast.makeText(getActivity(), "Error getting movie info", Toast.LENGTH_SHORT).show();
                // Exit back to the list screen
                getActivity().finish();
            }
        }

        public void onEvent(RetrofitError error) {
            Log.e(TAG, "Error getting movie info", error);
            Toast.makeText(getActivity(), error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            // Exit back to the list screen
            getActivity().finish();
        }

        /**
         * Some quick little reusable animation functions
         */

        public void popIn(View v, long delay) {
            if (v.getVisibility() != View.VISIBLE) {
                v.setVisibility(View.VISIBLE);
                ScaleAnimation anim = new ScaleAnimation(0f, 1f, 0f, 1f);
                anim.setDuration(750);
                anim.setStartOffset(delay);
                anim.setInterpolator(new BounceInterpolator());
                v.startAnimation(anim);
            }
        }

        public void fadeIn(View v, long delay) {
            if (v.getVisibility() != View.VISIBLE) {
                v.setVisibility(View.VISIBLE);
                AlphaAnimation anim = new AlphaAnimation(0f, 1f);
                anim.setDuration(750);
                anim.setStartOffset(delay);
                anim.setInterpolator(new DecelerateInterpolator());
                v.startAnimation(anim);
            }
        }

        public void lineAcross(View v, long delay) {
            if (v.getVisibility() != View.VISIBLE) {
                v.setVisibility(View.VISIBLE);
                ScaleAnimation anim = new ScaleAnimation(0f, 1f, 1f, 1f);
                anim.setDuration(750);
                anim.setStartOffset(delay);
                anim.setInterpolator(new DecelerateInterpolator());
                v.startAnimation(anim);
            }
        }

        public void fadeAndSlideDownIn(View v, long delay) {
            if (v.getVisibility() != View.VISIBLE) {
                v.setVisibility(View.VISIBLE);
                AnimationSet as = new AnimationSet(true);
                AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
                TranslateAnimation trans = new TranslateAnimation(0f, 0f, -100f, 0f);

                as.addAnimation(alpha);
                as.addAnimation(trans);
                as.setDuration(750);
                as.setStartOffset(delay);
                as.setInterpolator(new DecelerateInterpolator());
                v.startAnimation(as);
            }
        }
    }
}
