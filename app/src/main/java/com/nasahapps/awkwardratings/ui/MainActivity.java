package com.nasahapps.awkwardratings.ui;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.client.Firebase;
import com.malinskiy.superrecyclerview.OnMoreListener;
import com.malinskiy.superrecyclerview.SuperRecyclerView;
import com.nasahapps.awkwardratings.R;
import com.nasahapps.awkwardratings.Utils;
import com.nasahapps.awkwardratings.model.Movie;
import com.nasahapps.awkwardratings.model.MovieRating;
import com.nasahapps.awkwardratings.service.NetworkHelper;
import com.nasahapps.awkwardratings.service.response.PopularMovieResponse;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import de.greenrobot.event.EventBus;


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
        private List<ParseObject> mMovies = new ArrayList<>(), mMovieRatings;
        private Map<String, ParseObject> mMovieRatingMap = new HashMap<>();
        private Firebase mFirebase;

        private List<Movie> movies = new ArrayList<>();
        private int pageCount = 1;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_main, container, false);

            Scanner s = new Scanner(getResources().openRawResource(R.raw.firebase));
            try {
                List<String> keys = new ArrayList<>();
                while (s.hasNext()) {
                    keys.add(s.next());
                }

            } finally {
                s.close();
            }

            mRecyclerView = (SuperRecyclerView) v.findViewById(R.id.list);
            // Lay out in a linear fashion (ala ListView)
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
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
            // When 10 items away from the end of the list, query for more
            mRecyclerView.setupMoreListener(new OnMoreListener() {
                @Override
                public void onMoreAsked(int numOfItems, int numBeforeMore, int currentItemPos) {
                    getMovies();
                }
            }, 10);

            // Used for getting dummy data, ignore
            EventBus.getDefault().register(this);

            // Make a Map out of our user's movie ratings, if any
            /*
            if (ParseUser.getCurrentUser() == null) {
                ParseUser user = new ParseUser();
                user.setUsername(UUID.randomUUID().toString());
                user.setPassword("");
                user.signUpInBackground(new SignUpCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e != null) {
                            Log.e(TAG, "Error signing up user", e);
                        } else {
                            mMovieRatings = ParseUser.getCurrentUser().getList("movieRatings");
                            if (mMovieRatings != null) {
                                for (ParseObject po : mMovieRatings) {
                                    mMovieRatingMap.put(po.getObjectId(), po);
                                }
                            } else {
                                mMovieRatings = new ArrayList<>();
                            }
                        }
                    }
                });
            } else {
                mMovieRatings = ParseUser.getCurrentUser().getList("movieRatings");
                if (mMovieRatings != null) {
                    for (ParseObject po : mMovieRatings) {
                        mMovieRatingMap.put(po.getObjectId(), po);
                    }
                } else {
                    mMovieRatings = new ArrayList<>();
                }
            }
            */

            //getMovies();
            createDummyData();

            return v;
        }

        /**
         * Registering/unregistering for listening for EventBus was needed for creating dummy data
         * gathered from themoviedb.com. createDummyData() and onEvent() are not used, and were
         * only used one time to make dummy data
         */

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

        public void createDummyData() {
            NetworkHelper.getInstance(getActivity()).getPopularMovies(pageCount);
        }

        public void onEvent(PopularMovieResponse resp) {
            if (resp != null) {
                movies.addAll(resp.getResults());
                pageCount++;
                if (pageCount < 20) {
                    NetworkHelper.getInstance(getActivity()).getPopularMovies(pageCount);
                } else {
                    List<ParseObject> objs = new ArrayList<>();
                    for (Movie m : movies) {
                        ParseObject obj = new ParseObject("Movie");
                        obj.put("adult", m.isAdult());
                        if (m.getBackdropPath() != null)
                            obj.put("backdropPath", m.getBackdropPath());
                        if (m.getPosterPath() != null)
                            obj.put("posterPath", m.getPosterPath());
                        if (m.getOriginalTitle() != null)
                            obj.put("originalTitle", m.getOriginalTitle());
                        if (m.getTitle() != null)
                            obj.put("title", m.getTitle());
                        if (m.getReleaseDate() != null)
                            obj.put("releaseDate", m.getReleaseDate());
                        objs.add(obj);
                    }
                    ParseObject.saveAllInBackground(objs);
                }
            }
        }

        public void getMovies() {
            // Query our db for the movies, 100 at a time (default)
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Movie");
            // Ordered by most recently updated
            query.orderByDescending("updatedAt");
            // For pagination
            query.setSkip(mMovies.size());
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
                /*
                List<ParseObject> movieRatings = ParseUser.getCurrentUser().getList("movieRatings");
                MovieRating rating = null;
                if (movieRatings == null || !movieRatings.contains(movie)) {
                    holder.yesAwkward.setBackgroundResource(R.drawable.transparent_button_bg);
                    holder.noAwkward.setBackgroundResource(R.drawable.transparent_button_bg);
                    rating = new MovieRating(movie, false);
                } else {
                    int index = movieRatings.indexOf(movie);
                    ParseObject movieRating = movieRatings.get(index);
                    if (movieRating.getBoolean("rated")) {
                        holder.yesAwkward.setBackgroundResource(R.drawable.green_button_bg);
                        holder.noAwkward.setBackgroundResource(R.drawable.transparent_button_bg);
                        rating = new MovieRating(movie, true, true);
                    } else {
                        holder.yesAwkward.setBackgroundResource(R.drawable.transparent_button_bg);
                        holder.noAwkward.setBackgroundResource(R.drawable.red_button_bg);
                        rating = new MovieRating(movie, true, false);
                    }
                }
                */
                boolean hasRated = mMovieRatingMap.containsKey(movie.getObjectId());
                if (hasRated) {
                    try {
                        ParseObject movieRating = mMovieRatingMap.get(movie.getObjectId());
                        if (movieRating.fetchIfNeeded().getBoolean("rating")) {
                            holder.yesAwkward.setBackgroundResource(R.drawable.green_button_bg);
                            holder.noAwkward.setBackgroundResource(R.drawable.transparent_button_bg);
                        } else {
                            holder.yesAwkward.setBackgroundResource(R.drawable.transparent_button_bg);
                            holder.noAwkward.setBackgroundResource(R.drawable.red_button_bg);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error fetching MovieRating object", e);
                        holder.yesAwkward.setBackgroundResource(R.drawable.transparent_button_bg);
                        holder.noAwkward.setBackgroundResource(R.drawable.transparent_button_bg);
                    }
                } else {
                    holder.yesAwkward.setBackgroundResource(R.drawable.transparent_button_bg);
                    holder.noAwkward.setBackgroundResource(R.drawable.transparent_button_bg);
                }

                holder.title.setText(movie.getString("title"));

                Number yes = movie.getNumber("awkwardYes");
                Number no = movie.getNumber("awkwardNo");
                if (yes == null) {
                    holder.rating.setText("0% awkward");
                } else if (no == null) {
                    holder.rating.setText("100% awkward");
                } else {
                    long percent = yes.longValue() * 100 / (yes.longValue() + no.longValue());
                    holder.rating.setText(percent + "% awkward");
                }

                if (movie.getString("posterPath") != null) {
                    final View background = holder.itemView;
                    final ImageView iv = holder.poster;
                    Uri uri = Uri.parse("https://image.tmdb.org/t/p/w150" + movie.getString("posterPath")
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
                //holder.yesAwkward.setTag(rating);
                //holder.noAwkward.setTag(rating);
                holder.yesAwkward.setTag(position);
                holder.noAwkward.setTag(position);

                setVoteClickListener(holder.yesAwkward, true);
                setVoteClickListener(holder.noAwkward, false);
            }

            @Override
            public int getItemCount() {
                return movies.size();
            }

            public void setVoteClickListener(View v, final boolean awkward) {
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = (int) v.getTag();
                        ParseObject movie = mMovies.get(position);
                        String voteKey = awkward ? "awkwardYes" : "awkwardNo";

                        if (mMovieRatingMap.containsKey(movie.getObjectId())) {
                            // User has voted on this movie before
                            // First check if user pressed the same vote button
                            // If so, unvote
                            ParseObject movieRating = mMovieRatingMap.get(movie.getObjectId());
                            try {
                                if (awkward == movieRating.fetchIfNeeded().getBoolean("rating")) {
                                    // e.g. user voted yes before, now is unvoting yes
                                    // First decrement a vote for this movie
                                    if (movie.fetchIfNeeded().getNumber(voteKey) != null) {
                                        movie.put(voteKey, movie.getNumber(voteKey).longValue() - 1);
                                    }
                                    movie.saveInBackground(new SaveCallback() {
                                        @Override
                                        public void done(ParseException e) {
                                            if (e != null)
                                                Log.e(TAG, "Error saving movie", e);
                                        }
                                    });

                                    // Then remove this rating from the user
                                    mMovieRatingMap.remove(movie.getObjectId());
                                    mMovieRatings = new ArrayList<>(mMovieRatingMap.values());
                                    ParseUser.getCurrentUser().put("movieRatings", mMovieRatings);
                                    ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
                                        @Override
                                        public void done(ParseException e) {
                                            if (e != null)
                                                Log.e(TAG, "Error saving user", e);
                                        }
                                    });
                                } else {
                                    // e.g. user voted yes before, now is voting no
                                    // First decrement the original vote for this movie
                                    String otherKey = awkward ? "awkwardNo" : "awkwardYes";
                                    if (movie.fetchIfNeeded().getNumber(otherKey) != null) {
                                        movie.put(otherKey, movie.getNumber(otherKey).longValue() - 1);
                                    }
                                    // Then increment the new vote
                                    if (movie.getNumber(voteKey) != null) {
                                        movie.increment(voteKey);
                                    } else {
                                        movie.put(voteKey, 1);
                                    }
                                    movie.saveInBackground(new SaveCallback() {
                                        @Override
                                        public void done(ParseException e) {
                                            if (e != null)
                                                Log.e(TAG, "Error saving movie", e);
                                        }
                                    });

                                    // Then change the rating to the user
                                    movieRating.put("rating", awkward);
                                    movieRating.saveInBackground(new SaveCallback() {
                                        @Override
                                        public void done(ParseException e) {
                                            if (e != null)
                                                Log.e(TAG, "Error saving movie rating", e);
                                        }
                                    });
                                    mMovieRatingMap.put(movie.getObjectId(), movieRating);

                                    // And update the user's list of movie ratings
                                    mMovieRatings = new ArrayList<>(mMovieRatingMap.values());
                                    ParseUser.getCurrentUser().put("movieRatings", mMovieRatings);
                                    ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
                                        @Override
                                        public void done(ParseException e) {
                                            if (e != null)
                                                Log.e(TAG, "Error saving user", e);
                                        }
                                    });
                                }

                                // Finally, update our buttons
                                mRecyclerView.getAdapter().notifyItemChanged(position);
                            } catch (Exception e) {
                                Log.e(TAG, "Error fetching MovieRating or Movie object", e);
                            }
                        } else {
                            // User hasn't voted on this movie before
                            // So first increment a vote for this movie
                            try {
                                if (movie.fetchIfNeeded().getNumber(voteKey) != null) {
                                    movie.increment(voteKey);
                                } else {
                                    movie.put(voteKey, 1);
                                }
                                movie.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e != null)
                                            Log.e(TAG, "Error saving movie", e);
                                    }
                                });

                                // Then save this rating to the user
                                // Make the MovieRating object
                                ParseObject movieRating = new ParseObject("MovieRating");
                                movieRating.put("movie", movie);
                                movieRating.put("rating", awkward);
                                movieRating.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e != null)
                                            Log.e(TAG, "Error saving movie rating", e);
                                    }
                                });

                                // Then add it to our Map as well as the user's list of movie ratings
                                mMovieRatingMap.put(movie.getObjectId(), movieRating);
                                mMovieRatings.add(movieRating);
                                ParseUser.getCurrentUser().put("movieRatings", mMovieRatings);
                                ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e != null)
                                            Log.e(TAG, "Error saving user", e);
                                    }
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "Error fetching Movie object", e);
                            }

                            // Then finally, let's update our buttons
                            try {
                                mRecyclerView.getAdapter().notifyItemChanged(position);
                            } catch (Exception e) {
                                Log.e(TAG, "Error notifying item changed at position " + position);
                            }
                        }
                    }
                });
            }

            public void setVoteClickListener(final boolean awkward, View v, final View otherButton) {
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final MovieRating rating = (MovieRating) v.getTag();
                        List<ParseObject> movieRatings = ParseUser.getCurrentUser().getList("movieRatings");
                        if (movieRatings == null) {
                            // User hasn't voted on anything yet
                            // This will be their very first vote!
                            // So add a yes/no vote to the movie
                            addVote(rating.getMovie(), awkward);

                            // Then add this movie rating to the user
                            movieRatings = new ArrayList<ParseObject>();
                            addMovieToUser(movieRatings, rating, awkward);

                            // And change the AWK bg to green/red
                            v.setBackgroundResource(awkward ? R.drawable.green_button_bg : R.drawable.red_button_bg);
                        } else {
                            if (rating.isRated()) {
                                for (ParseObject po : movieRatings) {
                                    try {
                                        if (po.fetchIfNeeded().getParseObject("movie") == rating.getMovie()) {
                                            // User has voted on this movie before, so remove the vote if it's the same option
                                            if (rating.isAwkward() == awkward) {
                                                removeMovieFromUser(movieRatings, rating);

                                                // Decrement a yes/no vote
                                                removeVote(rating.getMovie(), awkward);

                                                // And change the button bg back to black
                                                v.setBackgroundResource(R.drawable.transparent_button_bg);
                                                return;
                                            } else {
                                                // User voted one way, then clicked the other button
                                                // Increment the new vote
                                                addVote(rating.getMovie(), awkward);

                                                // Save the updated movie to user list
                                                rating.getMovie().put("rating", awkward);
                                                rating.setAwkward(awkward);
                                                ParseUser.getCurrentUser().put("movieRatings", movieRatings);
                                                ParseUser.getCurrentUser().saveInBackground();

                                                // Decrement the other vote
                                                removeVote(rating.getMovie(), !awkward);

                                                // Then change new bg to green, old bg to black
                                                v.setBackgroundResource(awkward ? R.drawable.green_button_bg : R.drawable.red_button_bg);
                                                otherButton.setBackgroundResource(R.drawable.transparent_button_bg);
                                            }
                                        }
                                    } catch (ParseException e) {
                                        Log.e(TAG, "Error fetching object", e);
                                        v.setBackgroundResource(R.drawable.transparent_button_bg);
                                        return;
                                    }
                                }
                            } else {
                                // User hasn't vote on this movie before, so add a yes/no vote
                                addVote(rating.getMovie(), awkward);

                                // Then add this movie rating to the user
                                addMovieToUser(movieRatings, rating, awkward);

                                // And change the AWK bg to green/red
                                v.setBackgroundResource(awkward ? R.drawable.green_button_bg : R.drawable.red_button_bg);
                            }
                        }
                    }
                });
            }

            public void addVote(ParseObject movie, boolean awkward) {
                String key;
                if (awkward)
                    key = "awkwardYes";
                else
                    key = "awkwardNo";

                if (movie.getNumber(key) != null)
                    movie.increment(key);
                else
                    movie.put(key, 1);
                movie.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e != null)
                            Log.e(TAG, "Error saving movie", e);
                    }
                });
            }

            public void removeVote(ParseObject movie, boolean awkward) {
                String key;
                if (awkward)
                    key = "awkwardYes";
                else
                    key = "awkwardNo";

                if (movie.getNumber(key) != null) {
                    movie.put(key, movie.getNumber(key).longValue() - 1);
                    movie.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null)
                                Log.e(TAG, "Error saving movie", e);
                        }
                    });
                }
            }

            public void addMovieToUser(List<ParseObject> ratings, MovieRating rating, boolean awkward) {
                ParseObject movieRating = new ParseObject("MovieRating");
                movieRating.put("movie", rating.getMovie());
                movieRating.put("rating", awkward);
                rating.setAwkward(awkward);
                rating.setRated(true);
                ratings.add(movieRating);
                ParseUser.getCurrentUser().put("movieRatings", ratings);
                ParseUser.getCurrentUser().saveInBackground();
            }

            public void removeMovieFromUser(List<ParseObject> ratings, MovieRating rating) {
                ratings.remove(rating.getMovie());
                ParseUser.getCurrentUser().put("movieRatings", ratings);
                ParseUser.getCurrentUser().saveInBackground();
                rating.setRated(false);
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
