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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.malinskiy.superrecyclerview.OnMoreListener;
import com.malinskiy.superrecyclerview.SuperRecyclerView;
import com.nasahapps.awkwardratings.R;
import com.nasahapps.awkwardratings.Utils;
import com.nasahapps.awkwardratings.model.Movie;
import com.nasahapps.awkwardratings.service.NetworkHelper;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;


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

        private List<Movie> movies = new ArrayList<>();
        private int pageCount = 1;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_main, container, false);

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
            //EventBus.getDefault().register(this);

            getMovies();

            return v;
        }

        /**
         * Registering/unregistering for listening for EventBus was needed for creating dummy data
         * gathered from themoviedb.com. createDummyData() and onEvent() are not used, and were
         * only used one time to make dummy data
         */

        /*
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
        */
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

                holder.title.setText(movie.getString("title"));

                Number yes = movie.getNumber("awkwardYes");
                Number no = movie.getNumber("awkwardNo");
                if (yes == null && no == null) {
                    holder.rating.setText("0% awkward");
                } else if (yes != null && no == null) {
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
                holder.yesAwkward.setTag(movie);
                holder.noAwkward.setTag(movie);

                holder.yesAwkward.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ParseObject movie = (ParseObject) v.getTag();
                        if (movie.getNumber("awkwardYes") != null)
                            movie.increment("awkwardYes");
                        else
                            movie.put("awkwardYes", 1);
                        movie.saveInBackground();
                    }
                });
                holder.noAwkward.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ParseObject movie = (ParseObject) v.getTag();
                        if (movie.getNumber("awkwardNo") != null)
                            movie.increment("awkwardNo");
                        else
                            movie.put("awkwardNo", 1);
                        movie.saveInBackground();
                    }
                });
            }

            @Override
            public int getItemCount() {
                return movies.size();
            }

            public void colorBackground(ImageView iv, final View background) {
                Palette.generateAsync(Utils.getImageViewBitmap(iv), new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette p) {
                        // In order, get a dark muted color. If not exists, get muted, followed by dark vibrant, then black
                        background.setBackgroundColor(p.getDarkMutedColor(p.getMutedColor(p.getDarkVibrantColor(0xff000000))));
                    }
                });
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
