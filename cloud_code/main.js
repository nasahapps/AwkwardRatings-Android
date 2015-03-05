var TMDB_KEY = "Ha! Nice try, you're not getting my API key!";
var Movie = Parse.Object.extend("Movie");

Parse.Cloud.job("get_movies", function(request, status) {
	Parse.Cloud.useMasterKey();
	var movieResponses = [];
	var requestPromises = [];
	// Get the top 25 pages of results (20 results per page) which is 500 movies
	for (var i = 25; i > 0; i--) {
		requestPromises.push(getPopular(i).then(function(response) {
			movieResponses = movieResponses.concat(response.data.results);
		}, function(error) {
			console.error("Error " + error.code + " saving request: " + error.message);
		}));
	}

	Parse.Promise.when(requestPromises).then(function() {
		var moviePromises = [];
		for (var i = 0; i < movieResponses.length; i++) {
			var movie = new Movie();
			var json = movieResponses[i];
			movie.set("adult", json.adult);
			movie.set("backdrop_path", json.backdrop_path);
			movie.set("movie_id", json.id);
			movie.set("original_title", json.original_title);
			movie.set("release_date", json.release_date);
			movie.set("poster_path", json.poster_path);
			movie.set("title", json.title);
			moviePromises.push(movie.save(null, {
				success: function(movieObj) {
					
				}, error: function(movieObj, error) {
					console.error("Error " + error.code + " saving movie: " + error.message);
				}
			}));
		}

		Parse.Promise.when(moviePromises).then(function() {
			status.success("Saved all " + moviePromises.length + " movies");
		}, function(error) {
			status.error("Error " + error.code + " saving movies to Parse: " + error.message);
		});
	}, function(error) {
		status.error("Error " + error.code + " getting movies from REST API: " + error.message);
	});
});

// Check if movie already is in DB
Parse.Cloud.beforeSave("Movie", function(request, response) {
	if (!request.object.get("movie_id")) {
		response.error("Movie must have a movie_id");
	} else {
		var query = new Parse.Query(Movie);
		query.equalTo("movie_id", request.object.get("movie_id"));
		query.first({
			success: function(movie) {
				if (movie) {
					// Movie already exists, are we updating the vote?
					// First check if the new object is brand new not currently existing
					// If it's brand new, ignore
					if (request.object.get("awkward_yes") == null && request.object.get("awkward_no") == null) {
						response.error("This movie already exists in the DB");
					} else {
						// Else, let it save
						// But first make sure voting wouldn't let it go under 0
						if (request.object.get("awkward_yes") < 0) {
							request.object.set("awkward_yes", 0);
						}
						if (request.object.get("awkward_no") < 0) {
							request.object.set("awkward_no", 0);
						}
						response.success();
					}
				} else {
					// Movie doesn't already exist, set votes to 0
					request.object.set("awkward_yes", 0);
					request.object.set("awkward_no", 0);
					response.success();
				}
			}, error: function(error) {
				response.error("Could not validate uniqueness for this object");
			}
		});
	}
});

function getPopular(page) {
	return Parse.Cloud.httpRequest({
		url: 'https://api.themoviedb.org/3/movie/popular',
		params: {
			api_key: TMDB_KEY,
			page: page
		}
	});
}

function getNowPlaying(page) {
	return Parse.Cloud.httpRequest({
		url: 'https://api.themoviedb.org/3/movie/now_playing',
		params: {
			api_key: TMDB_KEY,
			page: page
		}
	});
}

function keyExists(json, key) {
	if (json.hasOwnProperty(key))
		return true;
	else return false;
}