package com.nasahapps.awkwardratings.ui;

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.transition.Explode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.malinskiy.superrecyclerview.OnMoreListener;
import com.malinskiy.superrecyclerview.SuperRecyclerView;
import com.nasahapps.awkwardratings.PreferencesHelper;
import com.nasahapps.awkwardratings.R;
import com.nasahapps.awkwardratings.Utils;
import com.nasahapps.awkwardratings.model.Movie;
import com.nasahapps.awkwardratings.model.MovieRating;
import com.nasahapps.awkwardratings.service.NetworkHelper;
import com.nasahapps.awkwardratings.service.VoteHelper;
import com.nasahapps.awkwardratings.service.response.MovieResponse;
import com.nasahapps.awkwardratings.ui.custom.HidingScrollListener;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import de.psdev.licensesdialog.LicensesDialog;
import jp.wasabeef.recyclerview.animators.adapters.AlphaInAnimationAdapter;
import jp.wasabeef.recyclerview.animators.adapters.ScaleInAnimationAdapter;
import jp.wasabeef.recyclerview.animators.adapters.SlideInBottomAnimationAdapter;
import retrofit.RetrofitError;


public class MainActivity extends ActionBarActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    @TargetApi(21)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Utils.isAtApiLevel(21)) {
            // Set window transitions for when user clicks a movie
            getWindow().setAllowEnterTransitionOverlap(true);
            getWindow().setAllowReturnTransitionOverlap(true);
            getWindow().setExitTransition(new Explode());
        }
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
        private int mLastPosition, mRateCounter;
        // Using a ListView instead of RecyclerView because I've had problems in the past
        // with RecyclerViews crashing whenever its parent layout in XML has
        // android:animateLayoutChanges="true". ListViews don't have this problem
        // Plus, we'll only be showing at most 20 movies per search query so
        // large scale lists are not a problem here
        private ListView mSearchListView;
        private List<Movie> mSearchedMovies;
        private Menu mMenu;
        private String mLastQuery;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_main, container, false);
            // So the fragment isn't recreated on rotation
            setRetainInstance(true);
            // And we attach a menu for this fragment
            setHasOptionsMenu(true);

            // To implement the hiding ActionBar, we must have the style of this fragment's
            // Activity not have an ActionBar and put in a Toolbar in the xml layout so we can
            // manually show/hide the Toolbar as the user scrolls through the list of movies
            mToolbar = (Toolbar) v.findViewById(R.id.toolbar);
            ((ActionBarActivity) getActivity()).setSupportActionBar(mToolbar);

            mRecyclerView = (SuperRecyclerView) v.findViewById(R.id.list);
            // When in portrait
            if (Utils.isPortrait(getActivity())) {
                // Phones laid out as a list
                if (!Utils.isTablet(getActivity())) {
                    mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                } else { // Tablets in a grid of 2 columns
                    mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
                }
            } else {
                // When in landscape, a horizontal list
                mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
            }
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
            // Only available in portrait since in landscape you swipe left-right, not up-down
            if (Utils.isPortrait(getActivity())) {
                mRecyclerView.setRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        getMovies();
                    }
                });
            }
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
                // Scale in, alpha in, and slide in from bottom new items
                mRecyclerView.setAdapter(new SlideInBottomAnimationAdapter(
                        new ScaleInAnimationAdapter(new AlphaInAnimationAdapter(adapter))));
            }

            // The ListView holding our search results
            mSearchListView = (ListView) v.findViewById(R.id.searchListView);
            // Repopulate the listview if user rotated the device
            // On rotation, mSearchedMovies is retained and thus if it wasn't null before rotation,
            // it won't be null after rotation
            if (mSearchedMovies != null) {
                SearchMovieAdapter adapter = new SearchMovieAdapter(getActivity(), mSearchedMovies);
                mSearchListView.setAdapter(adapter);
                mSearchListView.setVisibility(View.VISIBLE);
            }
            mSearchListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // When item is clicked, first check if the movie the user wants to see is in our
                    // Parse DB
                    final Movie m = mSearchedMovies.get(position);
                    ParseQuery<ParseObject> query = ParseQuery.getQuery("Movie");
                    query.whereEqualTo("movie_id", m.getId());
                    query.getFirstInBackground(new GetCallback<ParseObject>() {
                        @Override
                        public void done(ParseObject parseObject, ParseException e) {
                            if (e == null) {
                                // Not sure if no object would return a null ParseObject or
                                // return a ParseException saying no results returned. So
                                // implementing for both cases
                                if (parseObject != null) {
                                    // We have this movie in our database, open this movie's page
                                    Intent i = new Intent(getActivity(), MovieActivity.class);
                                    i.putExtra(MovieActivity.EXTRA_ID, m.getId());
                                    startActivity(i);
                                } else {
                                    // Add this movie to our database so users can vote on it
                                    ParseObject movie = new ParseObject("Movie");
                                    movie.put("adult", m.isAdult());
                                    movie.put("backdrop_path", m.getBackdropPath());
                                    movie.put("movie_id", m.getId());
                                    movie.put("original_title", m.getOriginalTitle());
                                    movie.put("release_date", m.getReleaseDate());
                                    movie.put("poster_path", m.getPosterPath());
                                    movie.put("title", m.getTitle());
                                    movie.saveInBackground(new SaveCallback() {
                                        @TargetApi(21)
                                        @Override
                                        public void done(ParseException e) {
                                            // If everything went well, then open up the movie page
                                            if (e == null) {
                                                Intent i = new Intent(getActivity(), MovieActivity.class);
                                                i.putExtra(MovieActivity.EXTRA_ID, m.getId());
                                                if (Utils.isAtApiLevel(21)) {
                                                    // Call this version of startActivity to allow the transition animations
                                                    // to happen
                                                    getActivity().startActivity(i,
                                                            ActivityOptions.makeSceneTransitionAnimation(getActivity()).toBundle());
                                                } else {
                                                    startActivity(i);
                                                }
                                            } else {
                                                Utils.showError(getActivity(), TAG, "Error saving new movie", e,
                                                        e.getLocalizedMessage());
                                            }
                                        }
                                    });
                                }
                            } else {
                                Log.e(TAG, "Error querying movies", e);
                                // Add this movie to our database so users can vote on it
                                ParseObject movie = new ParseObject("Movie");
                                movie.put("adult", m.isAdult());
                                if (m.getBackdropPath() != null)
                                    movie.put("backdrop_path", m.getBackdropPath());
                                movie.put("movie_id", m.getId());
                                if (m.getOriginalTitle() != null)
                                    movie.put("original_title", m.getOriginalTitle());
                                if (m.getReleaseDate() != null)
                                    movie.put("release_date", m.getReleaseDate());
                                if (m.getPosterPath() != null)
                                    movie.put("poster_path", m.getPosterPath());
                                if (m.getTitle() != null)
                                    movie.put("title", m.getTitle());
                                movie.saveInBackground(new SaveCallback() {
                                    @TargetApi(21)
                                    @Override
                                    public void done(ParseException e) {
                                        // If everything went well, then open up the movie page
                                        if (e == null) {
                                            Intent i = new Intent(getActivity(), MovieActivity.class);
                                            i.putExtra(MovieActivity.EXTRA_ID, m.getId());
                                            if (Utils.isAtApiLevel(21)) {
                                                // Call this version of startActivity to allow the transition animations
                                                // to happen
                                                getActivity().startActivity(i,
                                                        ActivityOptions.makeSceneTransitionAnimation(getActivity()).toBundle());
                                            } else {
                                                startActivity(i);
                                            }
                                        } else {
                                            Utils.showError(getActivity(), TAG, "Error saving new movie", e,
                                                    e.getLocalizedMessage());
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            });

            mRateCounter = PreferencesHelper.getInstance(getActivity())
                    .getInt(PreferencesHelper.KEY_RATE_APP_COUNTER, 0);
            mRateCounter++;
            PreferencesHelper.getInstance(getActivity()).putInt(PreferencesHelper.KEY_RATE_APP_COUNTER, mRateCounter);
            Utils.showRateDialog(getActivity(), mRateCounter);

            EventBus.getDefault().register(this);

            // Only get movies if this is the first time loading (not after rotation)
            if (savedInstanceState == null) {
                // Also load user's movie ratings from prefs
                mMovieRatings = PreferencesHelper.getInstance(getActivity()).loadMovieRatings();
                getMovies();
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
        public void onResume() {
            super.onResume();
            // When coming back from a movie page, refresh the recycler view in case any votes
            // were changed
            mMovieRatings = PreferencesHelper.getInstance(getActivity()).loadMovieRatings();
            if (mRecyclerView != null && mRecyclerView.getAdapter() != null) {
                mRecyclerView.getAdapter().notifyItemChanged(mLastPosition);
            }
        }

        @Override
        public void onStop() {
            super.onStop();
            if (EventBus.getDefault().isRegistered(this))
                EventBus.getDefault().unregister(this);
        }

        @Override
        public void onPrepareOptionsMenu(Menu menu) {
            // If user was searching and rotated, make sure the search bar is still expanded
            if (mMenu != null && mSearchedMovies != null) {
                MenuItem searchItem = mMenu.findItem(R.id.searchMenuItem);
                searchItem.expandActionView();
                SearchView searchView = (SearchView) menu.findItem(R.id.searchMenuItem).getActionView();
                searchView.setQuery(mLastQuery, false);
            }
            super.onPrepareOptionsMenu(menu);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            // Inflate the options menu from XML
            inflater.inflate(R.menu.menu_main, menu);
            mMenu = menu;

            // Get the SearchView and set the searchable config
            SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
            final SearchView searchView = (SearchView) menu.findItem(R.id.searchMenuItem).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
            searchView.setIconifiedByDefault(true); // Show as an icon by default
            searchView.setQueryHint("Search movies"); // For some reason, setting this in xml doesn't work
            searchView.setQuery(mLastQuery, false); // If user rotated, pre-fill the search bar with
            // the user's last query
            // Also side note, when the user presses the "return" key on the keyboard, this doesn't
            // do a search simply because a search is already being done as they type. Pressing "return"
            // just hides the keyboard if it's showing

            MenuItem searchItem = menu.findItem(R.id.searchMenuItem);
            // This was added because some devices wouldn't get rid of the SearchListView
            // when the back button was clicked. This fixes that.
            MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem menuItem) {
                    mSearchListView.setVisibility(View.VISIBLE);
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                    mSearchListView.setAdapter(null);
                    mSearchListView.setVisibility(View.GONE);
                    mSearchedMovies = null;
                    return true;
                }
            });
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    if (s.equals("")) {
                        mSearchListView.setAdapter(null);
                    } else {
                        mLastQuery = s;
                        NetworkHelper.getInstance(getActivity()).searchMovie(s);
                    }

                    return true;
                }
            });

            super.onCreateOptionsMenu(menu, inflater);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.github:
                    // Open the Github to view this app's source code
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("https://github.com/nasahapps/AwkwardRatings-Android"));
                    if (i.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(i);
                    } else {
                        Toast.makeText(getActivity(), "No web browser installed", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                case R.id.openSource:
                    // Open a dialog to give credit to 3rd-party libraries
                    new LicensesDialog.Builder(getActivity()).setNotices(R.raw.notices).build().show();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

        /**
         * For getting movies from our Parse DB
         */
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
                            // Scale in, alpha in, and slide in from bottom new items
                            mRecyclerView.setAdapter(new SlideInBottomAnimationAdapter(
                                    new ScaleInAnimationAdapter(new AlphaInAnimationAdapter(adapter))));
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

        /**
         * Used with EventBus
         */

        public void onEvent(MovieResponse response) {
            if (response.getResults() != null) {
                // Fill our search ListView with search results
                mSearchedMovies = response.getResults();
                SearchMovieAdapter adapter = new SearchMovieAdapter(getActivity(), mSearchedMovies);
                mSearchListView.setAdapter(adapter);
            }
        }

        public void onEvent(RetrofitError error) {
            Toast.makeText(getActivity(), error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }

        public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.ViewHolder> {
            private List<ParseObject> movies;

            public MovieAdapter(List<ParseObject> movies) {
                this.movies = movies;
            }

            @Override
            public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
                View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_movie, viewGroup, false);
                // If in landscape (tablet or phone), alter the size of the view to adjust
                // proportionally to the movie poster (27:40)
                if (!Utils.isPortrait(getActivity())) {
                    Point size = Utils.getScreenDimensions(getActivity());
                    // Get the adjusted height of the screen (screen height - action bar height)
                    // (Had to multiply action bar height by 2 for some reason to get the correct height)
                    size.y -= (Utils.getActionBarHeight(getActivity()) * 2);
                    // And get the width based on that height
                    int newWidth = 27 * size.y / 40;
                    // Set these values to each list item
                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(newWidth, size.y);
                    v.setLayoutParams(lp);
                } else if (Utils.isTablet(getActivity())) {
                    // Else if a tablet in portrait, do the same thing, but adjust to the width of the
                    // view's parent width (should be half the screen width since it's laid out in
                    // 2 columns
                    Point size = Utils.getScreenDimensions(getActivity());
                    int newHeight = (size.x / 2) * 40 / 27;
                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(v.getWidth(), newHeight);
                    v.setLayoutParams(lp);
                }
                return new ViewHolder(v);
            }

            @TargetApi(21)
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

                // Set awkward rating based on votes
                long percent = VoteHelper.getVote(movie);
                if (percent == -1)
                    holder.rating.setText("No rating");
                else
                    holder.rating.setText(percent + "% awkward");

                if (movie.getString("poster_path") != null) {
                    // Get movie poster and Palette color
                    // the background to be colored on phones is the entire bg of the view
                    // the background to be colored on tablets or phones in landscape
                    // is the bg that is under the text
                    final View background = (!Utils.isTablet(getActivity()) && Utils.isPortrait(getActivity()))
                            ? holder.itemView : holder.bottomLayout;
                    final ImageView iv = holder.poster;
                    Uri uri = Uri.parse("https://image.tmdb.org/t/p/w300" + movie.getString("poster_path")
                            + "?api_key=" + NetworkHelper.getInstance(getActivity()).getApiKey());
                    Picasso.with(getActivity()).load(uri).into(holder.poster, new Callback() {
                        @Override
                        public void onSuccess() {
                            Palette.generateAsync(Utils.getImageViewBitmap(iv), new Palette.PaletteAsyncListener() {
                                @Override
                                public void onGenerated(Palette p) {
                                    int color = p.getDarkMutedColor(p.getMutedColor(p.getDarkVibrantColor(0xff000000)));
                                    if (getActivity() != null
                                            && (!Utils.isPortrait(getActivity()) || Utils.isTablet(getActivity()))) {
                                        // Have the alpha be 0.7 in landscape (phones)
                                        // Or 0.7 for tablets in general
                                        // 0.7 alpha is 179/255
                                        int newColor = Color.argb(179, Color.red(color), Color.green(color), Color.blue(color));
                                        Utils.animateToColor(background, newColor);
                                    } else {
                                        Utils.animateToColor(background, color);
                                    }
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
                        if (Utils.isAtApiLevel(21)) {
                            // Call this version of startActivity to allow the transition animations
                            // to happen
                            getActivity().startActivity(i,
                                    ActivityOptions.makeSceneTransitionAnimation(getActivity()).toBundle());
                        } else {
                            startActivity(i);
                        }
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
                public View bottomLayout;

                public ViewHolder(View v) {
                    super(v);
                    title = (TextView) v.findViewById(R.id.title);
                    rating = (TextView) v.findViewById(R.id.rating);
                    poster = (ImageView) v.findViewById(R.id.poster);
                    yesAwkward = (Button) v.findViewById(R.id.awkwardButton);
                    noAwkward = (Button) v.findViewById(R.id.notAwkwardButton);
                    bottomLayout = v.findViewById(R.id.bottomLayout);
                }
            }
        }


        // Using a different adapter other than MainFragment.MovieAdapter to keep this implementation simple
        // Plus it uses a different layout
        public class SearchMovieAdapter extends ArrayAdapter<Movie> {
            public SearchMovieAdapter(Context c, List<Movie> movies) {
                super(c, 0, movies);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_movie_search, parent, false);
                }

                Movie m = getItem(position);

                // Set movie title
                TextView title = (TextView) convertView.findViewById(R.id.title);
                title.setText(m.getTitle());

                // and release date if there is one
                TextView releaseDate = (TextView) convertView.findViewById(R.id.releaseDate);
                releaseDate.setVisibility(View.VISIBLE);
                try {
                    if (m.getReleaseDate() != null && !m.getReleaseDate().isEmpty()) {
                        SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd");
                        Date date = originalFormat.parse(m.getReleaseDate());
                        SimpleDateFormat newFormat = new SimpleDateFormat("MMMM yyyy");
                        releaseDate.setText(newFormat.format(date));
                    } else {
                        releaseDate.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing date", e);
                    releaseDate.setVisibility(View.GONE);
                }

                // Set movie poster and Palette color
                final ImageView poster = (ImageView) convertView.findViewById(R.id.poster);
                final View background = convertView;
                Uri uri = Uri.parse("https://image.tmdb.org/t/p/w150" + m.getPosterPath()
                        + "?api_key=" + NetworkHelper.getInstance(getContext()).getApiKey());
                Picasso.with(getContext()).load(uri).into(poster, new Callback() {
                    @Override
                    public void onSuccess() {
                        Palette.generateAsync(Utils.getImageViewBitmap(poster), new Palette.PaletteAsyncListener() {
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

                return convertView;
            }
        }
    }
}
