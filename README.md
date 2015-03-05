# Awkward Ratings for Android - v. 0.2

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

Where the parse file is just a two-line file where the first line is your application ID and the second line is your client key, and the tmdb and youtube files are one-line files that contain their respective API keys.  

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

If you wish to try out the app as-is, just download and install Awkward-Ratings.apk in the root folder.
