# Awkward Ratings for Android - v. 0.2

[https://play.google.com/store/apps/details?id=com.nasahapps.awkwardratings](https://play.google.com/store/apps/details?id=com.nasahapps.awkwardratings)

Simple app that uses the REST APIs documented on [www.themoviedb.org](https://www.themoviedb.org/documentation/api). Specifically, it asks for the day's most popular movies (sorted by when that movie was last updated in my database) and allows users of the app to vote on which movies would be awkward to watch with parents (or vice versa). Results are presented in an endless RecyclerView.  

The idea for this app was inspired by [this Reddit post](http://redd.it/2xhq9g).

App demonstrates the use of REST APIs via [Retrofit](http://square.github.io/retrofit/), image gathering/caching via [Picasso](http://square.github.io/picasso/), and coloring each list item based on the image using the AppCompat-v7 Palette class.  

<h2>User Features</h2> 

* Vote on whether a movie would be awkward or not to watch with parents if you're young, or if you're of the older parental crowd, vote on which movies would be awkward to watch with kids/teenagers. It could go both ways (that's what she said).  
* View quick info on a movie such as release date (month and year) and a short overview on the movie's plot.
* View that movie's trailer (if available). _Warning: Could be nolstalgic_ 
* Search for movies

**The app requires an API key from [themoviedb.org](https://www.themoviedb.org/documentation/api), [Parse](https://www.parse.com), and [YouTube](https://developers.google.com/youtube/android/player/) in order to be used correctly.** When reading API keys in the app, the app creates a Scanner object and scans these files in the /app/source/main/res/raw folder:  

* parse
* tmdb
* youtube

where the parse file is just a two-line file where the first line is your application ID and the second line is your client key, and the tmdb and youtube files are one-line files that contain their respective API keys.  

<h2>Dev Features</h2>

For you devs out there, here I will highlight what exactly this app is doing and how I'm getting it done (for more info/details, look in the code)

<h4>MainActivity.class/MainFragment.class</h4> 

This view primarily shows the list of most-recently-updated movies from our Parse DB in a RecyclerView. For this app, I use [SuperRecyclerView](https://github.com/Malinskiy/SuperRecyclerView) for its ease of use in integrating SwipeRefreshLayout, an empty view, and for loading more items as the user scrolls down the list. Each list item contains two buttons for the user to vote on that particular movie (awkward or not awkward). 

The layout for the RecyclerView is different depending on the device and orientation. For phones in portrait, items are laid out in a vertical list using `LinearLayoutManager`, and in landscape, items are laid out in a horizontal list also using `LinearLayoutManager` but setting the orientation to `LinearLayoutManager.HORIZONTAL`. For tablets in portrait, items are laid out in a vertical grid of 2 columns using `GridLayoutManager`, and landscape for tablets are the same as for phones.

The code for the Toolbar-hiding/showing effect as the user scrolls was sampled from [this helpful blog by Michal Z.](http://mzgreen.github.io/2015/02/15/How-to-hideshow-Toolbar-when-list-is-scroling%28part1%29/)

Also, users are able to search for movies from this view from the Toolbar. This brings up a ListView that holds those results (Why ListView and not RecyclerView? Because RecyclerView has problems when fading in and out if its parent has `animateLayoutChanges=true` in the XML file. Plus, the ListView is guaranteed in this app to show at most 20 items while the main RecyclerView may hold thousands)

<h4>MovieActivity.class/MovieFragment.class</h4>

This view shows info on a selected movie. This info includes the movie's backdrop image, poster image, title, release date, awkwardness rating, and a brief overview of the plot. Users can click on the movie poster to enlarge it in a new PosterActivity instance, or click on the movie backdrop to view that movie's trailer (if available).  

<h4>Animations</h4>  

The animations in MainFragment are ItemAnimator animations made easy thanks to [Wasabeef's recyclerview-animators library](https://github.com/wasabeef/recyclerview-animators). In this app, a simple one-liner was made to achieve the effect you see: 

    mRecyclerView.setAdapter(new SlideInBottomAnimationAdapter(new ScaleInAnimationAdapter(new AlphaInAnimationAdapter(adapter))));  
    
This has each new item slide in from the bottom, scale in, and fade in as the user scrolls through the list.  

Pre-Lollipop, the animations in MovieFragment are short and simple (see the animation functions at the bottom of MainFragment.class). The three middle buttons "pop" in with a ScaleAnimation, the text fades in with an AlphaAnimation, the line "draws" itself across the page with a ScaleAnimation (scaling the X value from 0 to it's original value), and the poster fades and slides down into place with an AnimationSet containing both an AlphaAnimation and a TranslateAnimation.

Post-Lollipop, there are also transition animations. The transition from MainActivity to MovieActivity uses the built-in Explode transition  

    getWindow().setExitTransition(new Explode());

Also, the transition from MovieActivity to PosterActivity uses a shared element transition on the poster image

    getWindow().setSharedElementExitTransition(new ChangeImageTransform());
    
For more info, refer to the [Material Design Custom Animations](http://developer.android.com/training/material/animations.html) docs on the Android dev website.  

<h4>NetworkHelper.class</h4>  

NetworkHelper is the helper class for REST API calls. It's basically a wrapper for a [Retrofit](http://square.github.io/retrofit/) client. All non-image, non-video, and non-database calls are done through this class. 

Images are gotten using [Picasso](http://square.github.io/picasso/) and Uris. For example:  

    Uri uri = Uri.parse("https://image.tmdb.org/t/p/w300" + mMovie.getBackdropPath() + "?api_key=" + NetworkHelper.getInstance(getActivity()).getApiKey());
    Picasso.with(getActivity()).load(uri).into(mBackdrop);

This creates a new Uri pointed at the image URL and loads this image asynchronously into an ImageView.  

If you wanted to get a Palette color after loading the image:  

    Uri uri = Uri.parse("https://image.tmdb.org/t/p/w150" + mMovie.getPosterPath()  + "?api_key=" + NetworkHelper.getInstance(getActivity()).getApiKey());
    Picasso.with(getActivity()).load(uri).into(mPoster, new Callback() {
        @Override
        public void onSuccess() {
            Palette.generateAsync(Utils.getImageViewBitmap(mPoster), new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette p) {
                    int color = p.getDarkMutedColor(p.getMutedColor(p.getDarkVibrantColor(0xff000000)));
                }
            });
        }

        @Override
        public void onError() {

        }
    });
    
In that example, after getting the image, its Drawable is used to get a Palette of colors, and from there `int color` can be a dark muted color, a muted color, a dark vibrant color, or black, in order of what's available.

Videos use the YouTube API, which is a simple two-liner:  

    Intent i = YouTubeStandalonePlayer.createVideoIntent(getActivity(), key, mMovie.getVideoId(), 0, true, false);
    startActivity(i);
    
where the params in `createVideoIntent()` are your Context, YouTube API key, YouTube video ID, start time, if the video should automatically start playing, and if the video should play in a dialog overlayed the current Activity.  

Parse database network connection code is housed in the UI class files (which may be bad practice, my bad) except for the voting code. That itself is contained in the VoteHelper.class and contains the logic for adjusting movies in the Parse DB whenever a user votes (or unvotes).

<h2>Parse Database</h2>  

My Parse DB implementation is very simple, because it only consists of one class (table). So if you want this app to work as I have intended, in your Parse project you'll need a class named "Movie" with these fields (case-sensitive key, field type):  

* title (string)
* adult (boolean)
* awkward_no (number)
* awkward_yes (number)
* backdrop_path (string)
* movie_id (number)
* original_title (string)
* poster_path (string)
* release_date (string)
* video (boolean)

Also, I use Cloud Code that, every night, polls TMDB for the day's popular movies and adds them to my own DB if those movies don't already exist in my DB. You can view that code in the /cloud_code/main.js
