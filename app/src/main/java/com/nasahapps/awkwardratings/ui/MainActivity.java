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
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.malinskiy.superrecyclerview.OnMoreListener;
import com.malinskiy.superrecyclerview.SuperRecyclerView;
import com.nasahapps.awkwardratings.R;
import com.nasahapps.awkwardratings.Utils;
import com.nasahapps.awkwardratings.model.Movie;
import com.nasahapps.awkwardratings.service.NetworkHelper;
import com.nasahapps.awkwardratings.service.response.PopularMovieResponse;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

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
        private List<Movie> mMovies = new ArrayList<>();
        private Toolbar mToolbar;
        private int pageCount = 1;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_main, container, false);
            setRetainInstance(true);

            // Set our Toolbar as the support ActionBar
            mToolbar = (Toolbar) v.findViewById(R.id.toolbar);
            ((ActionBarActivity) getActivity()).setSupportActionBar(mToolbar);

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

            EventBus.getDefault().register(this);

            // Let's get our movies
            getMovies();

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

        public void getMovies() {
            NetworkHelper.getInstance(getActivity()).getPopularMovies(pageCount);
        }

        public void onEvent(PopularMovieResponse resp) {
            if (resp != null) {
                mMovies.addAll(resp.getResults());
                pageCount++;
                if (mRecyclerView.getAdapter() == null) {
                    // If just creating our list, create it with a new adapter
                    MovieAdapter adapter = new MovieAdapter(mMovies);
                    mRecyclerView.setAdapter(adapter);
                } else {
                    // Else, just notify our adapter of the newly added items
                    mRecyclerView.getAdapter().notifyDataSetChanged();
                }

                mRecyclerView.hideMoreProgress();
            }
        }

        public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.ViewHolder> {
            private List<Movie> movies;

            public MovieAdapter(List<Movie> movies) {
                this.movies = movies;
            }

            @Override
            public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
                View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_movie, viewGroup, false);
                return new ViewHolder(v);
            }

            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                Movie movie = movies.get(position);

                // Reset background to black when scrolling down
                holder.itemView.setBackgroundColor(Color.BLACK);

                holder.title.setText(movie.getTitle());

                if (movie.getPosterPath() != null) {
                    final View background = holder.itemView;
                    final ImageView iv = holder.poster;
                    Uri uri = Uri.parse("https://image.tmdb.org/t/p/w150" + movie.getPosterPath()
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
            }

            @Override
            public int getItemCount() {
                return movies.size();
            }

            public class ViewHolder extends RecyclerView.ViewHolder {
                public TextView title, rating;
                public ImageView poster;

                public ViewHolder(View v) {
                    super(v);
                    title = (TextView) v.findViewById(R.id.title);
                    rating = (TextView) v.findViewById(R.id.rating);
                    poster = (ImageView) v.findViewById(R.id.poster);
                }
            }
        }
    }
}
