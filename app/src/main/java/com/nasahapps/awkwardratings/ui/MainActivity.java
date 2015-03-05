package com.nasahapps.awkwardratings.ui;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.malinskiy.superrecyclerview.OnMoreListener;
import com.malinskiy.superrecyclerview.SuperRecyclerView;
import com.nasahapps.awkwardratings.PreferencesHelper;
import com.nasahapps.awkwardratings.R;
import com.nasahapps.awkwardratings.Utils;
import com.nasahapps.awkwardratings.model.MovieRating;
import com.nasahapps.awkwardratings.service.NetworkHelper;
import com.nasahapps.awkwardratings.service.VoteHelper;
import com.nasahapps.awkwardratings.ui.custom.HidingScrollListener;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class MainActivity extends ActionBarActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new MainFragment())
                    .commit();
        }
    }

    public static class MainFragment extends Fragment {

        private SuperRecyclerView mRecyclerView;
        private List<ParseObject> mMovies = new ArrayList<>();
        private Map<Integer, MovieRating> mMovieRatings;
        private Toolbar mToolbar;
        // For when we return from MovieActivity, we know which view in RecyclerView to
        // refresh in case user voted on that page
        private int mLastPosition;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_main, container, false);
            setRetainInstance(true);

            mToolbar = (Toolbar) v.findViewById(R.id.toolbar);
            ((ActionBarActivity) getActivity()).setSupportActionBar(mToolbar);

            mRecyclerView = (SuperRecyclerView) v.findViewById(R.id.list);
            // Lay out in a linear fashion (ala ListView)
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            // Our scroll listener to have it hide/show the Toolbar on scroll
            mRecyclerView.setOnScrollListener(new HidingScrollListener() {
                @Override
                public void onHide() {
                    mToolbar.animate().translationY(-mToolbar.getHeight()).setInterpolator(new AccelerateInterpolator(2));
                }

                @Override
                public void onShow() {
                    mToolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2));
                }
            });
            // Pull down to refresh list of movies
            mRecyclerView.setRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    getMovies();
                }
            });
            // And have a colorful spinning refresh animation
            mRecyclerView.setRefreshingColorResources(android.R.color.holo_orange_light,
                    android.R.color.holo_blue_light, android.R.color.holo_green_light,
                    android.R.color.holo_red_light);
            // And have it not hidden under the Toolbar
            mRecyclerView.getSwipeToRefresh().setProgressViewOffset(false, (int) Utils.dpToPixel(getResources(), 24),
                    (int) Utils.dpToPixel(getResources(), 24 + 48));
            // When 10 items away from the end of the list, query for more
            mRecyclerView.setupMoreListener(new OnMoreListener() {
                @Override
                public void onMoreAsked(int numOfItems, int numBeforeMore, int currentItemPos) {
                    getMovies();
                }
            }, 10);
            // Also, set the adapter if we already have movies (used if device was rotated)
            if (savedInstanceState != null) {
                MovieAdapter adapter = new MovieAdapter(mMovies);
                mRecyclerView.setAdapter(adapter);
            }

            // Only get movies if this is the first time loading
            if (savedInstanceState == null) {
                // Also load user's movie ratings from prefs
                mMovieRatings = PreferencesHelper.getInstance(getActivity()).loadMovieRatings();
                getMovies();
            }

            return v;
        }

        @Override
        public void onResume() {
            super.onResume();
            // When coming back from a movie page, refresh the recycler view in case any votes
            // were changed
            mMovieRatings = PreferencesHelper.getInstance(getActivity()).loadMovieRatings();
            if (mRecyclerView != null && mRecyclerView.getAdapter() != null) {
                mRecyclerView.getAdapter().notifyItemChanged(mLastPosition);
            }
        }

        public void getMovies() {
            // Query our db for the movies
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Movie");
            // Ordered by most recently updated
            query.orderByDescending("updatedAt");
            // For pagination
            query.setSkip(mMovies.size());
            // Get 1000 at a time
            query.setLimit(1000);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> parseObjects, ParseException e) {
                    mRecyclerView.hideMoreProgress();

                    if (e == null) {
                        if (mMovies.isEmpty()) {
                            // Create a new adapter to bind to our RecyclerView
                            mMovies.addAll(parseObjects);
                            MovieAdapter adapter = new MovieAdapter(mMovies);
                            mRecyclerView.setAdapter(adapter);
                        } else {
                            // Add our results to what we already have
                            mMovies.addAll(parseObjects);
                            mRecyclerView.getAdapter().notifyDataSetChanged();
                        }
                    } else {
                        Utils.showError(getActivity(), TAG, "Error querying movies", e, e.getLocalizedMessage());
                    }
                }
            });
        }

        public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.ViewHolder> {
            private List<ParseObject> movies;

            public MovieAdapter(List<ParseObject> movies) {
                this.movies = movies;
            }

            @Override
            public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
                View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_movie, viewGroup, false);
                return new ViewHolder(v);
            }

            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                ParseObject movie = movies.get(position);

                // Reset background to black when scrolling down
                holder.itemView.setBackgroundColor(Color.BLACK);
                // And set the button bgs depending on whether user voted or not for this movie
                boolean hasRated = mMovieRatings.containsKey(movie.getNumber("movie_id").intValue());
                if (hasRated) {
                    MovieRating movieRating = mMovieRatings.get(movie.getNumber("movie_id").intValue());
                    if (movieRating.isAwkward()) {
                        holder.yesAwkward.setBackgroundResource(R.drawable.red_button_bg);
                        holder.noAwkward.setBackgroundResource(R.drawable.transparent_button_bg);
                    } else {
                        holder.yesAwkward.setBackgroundResource(R.drawable.transparent_button_bg);
                        holder.noAwkward.setBackgroundResource(R.drawable.green_button_bg);
                    }
                } else {
                    holder.yesAwkward.setBackgroundResource(R.drawable.transparent_button_bg);
                    holder.noAwkward.setBackgroundResource(R.drawable.transparent_button_bg);
                }

                holder.title.setText(movie.getString("title"));

                long percent = VoteHelper.getVote(movie);
                if (percent == -1)
                    holder.rating.setText("No rating");
                else
                    holder.rating.setText(percent + "% awkward");

                if (movie.getString("poster_path") != null) {
                    final View background = holder.itemView;
                    final ImageView iv = holder.poster;
                    Uri uri = Uri.parse("https://image.tmdb.org/t/p/w150" + movie.getString("poster_path")
                            + "?api_key=" + NetworkHelper.getInstance(getActivity()).getApiKey());
                    Picasso.with(getActivity()).load(uri).into(holder.poster, new Callback() {
                        @Override
                        public void onSuccess() {
                            Palette.generateAsync(Utils.getImageViewBitmap(iv), new Palette.PaletteAsyncListener() {
                                @Override
                                public void onGenerated(Palette p) {
                                    int color = p.getDarkMutedColor(p.getMutedColor(p.getDarkVibrantColor(0xff000000)));
                                    Utils.animateToColor(background, color);
                                }
                            });
                        }

                        @Override
                        public void onError() {

                        }
                    });
                }

                // To keep track of which movie we're referring to when we press the yes/no buttons
                holder.yesAwkward.setTag(position);
                holder.noAwkward.setTag(position);
                holder.yesAwkward.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = (Integer) v.getTag();
                        ParseObject movie = mMovies.get(position);
                        VoteHelper.vote(getActivity(), movie, true, mMovieRatings);
                        // Finally, update our buttons
                        mRecyclerView.getAdapter().notifyItemChanged(position);
                    }
                });
                holder.noAwkward.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = (Integer) v.getTag();
                        ParseObject movie = mMovies.get(position);
                        VoteHelper.vote(getActivity(), movie, false, mMovieRatings);
                        // Finally, update our buttons
                        mRecyclerView.getAdapter().notifyItemChanged(position);
                    }
                });

                //setVoteClickListener(holder.yesAwkward, true);
                //setVoteClickListener(holder.noAwkward, false);

                // And a click listener for the entire item itself to bring up a detailed movie page
                holder.itemView.setTag(position);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = (int) v.getTag();
                        mLastPosition = position;
                        ParseObject movie = mMovies.get(position);
                        Intent i = new Intent(getActivity(), MovieActivity.class);
                        i.putExtra(MovieActivity.EXTRA_ID, movie.getNumber("movie_id").intValue());
                        startActivity(i);
                    }
                });
            }

            @Override
            public int getItemCount() {
                return movies.size();
            }

            public class ViewHolder extends RecyclerView.ViewHolder {
                public TextView title, rating;
                public ImageView poster;
                public Button yesAwkward, noAwkward;

                public ViewHolder(View v) {
                    super(v);
                    title = (TextView) v.findViewById(R.id.title);
                    rating = (TextView) v.findViewById(R.id.rating);
                    poster = (ImageView) v.findViewById(R.id.poster);
                    yesAwkward = (Button) v.findViewById(R.id.awkwardButton);
                    noAwkward = (Button) v.findViewById(R.id.notAwkwardButton);
                }
            }
        }
    }
}
