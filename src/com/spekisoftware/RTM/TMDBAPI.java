package com.spekisoftware.RTM;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Hashtable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class TMDBAPI
{
    private final static String TMDB_API_KEY                 = "eynu3u8gvjcfgn5hhxhk4wt8kgkxyakw3";

    private final static String APIKEY_TOKEN                 = "{{APIKEY}}";
    private final static String QUERY_TOKEN                  = "{{QUERY}}";

    private final static String MOVIE_SEARCH_URL             = "http://api.themoviedb.org/2.1/Movie.search/en/json/"
                                                                     + APIKEY_TOKEN + "/" + QUERY_TOKEN;
    private final static String SEARCH_MOVIE_NO_RESULT_TOKEN = "[\"Nothing found.\"]";

    public interface AutocompleteCallback
    {
        public void callback(ArrayList<String> result);
    }

    public void AutocompleteMovies(String queryString, final AutocompleteCallback onSuccess,
            final RESTHandler.RESTCallback onError)
    {
        if (onSuccess == null) { return; }

        final String LOGTAG = "AutocompleteMovies";

        this.SearchMovie(queryString, new SearchMovieCallback()
        {

            @Override
            public void callback(ArrayList<TMDBMovie> result)
            {
                ArrayList<String> movieList = new ArrayList<String>();

                for (TMDBMovie movie : result)
                {
                    movieList.add(movie.name);
                }

                if (movieList.size() == 0)
                {
                    movieList.add(ReminderApplication.getContext().getString(R.string.NoMovieSuggestions));
                }

                Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format("Calling Autocomplete success callback with %d movies", movieList.size()));

                onSuccess.callback(movieList);
            }

        }, onError);
    }

    public void AutocompleteMovies_old(String queryString, final AutocompleteCallback onSuccess,
            final RESTHandler.RESTCallback onError)
    {
        if (onSuccess == null) { return; }

        final String LOGTAG = "AutocompleteMovies";

        Hashtable<String, String> tokens = new Hashtable<String, String>();

        tokens.put(APIKEY_TOKEN, TMDB_API_KEY);
        tokens.put(QUERY_TOKEN, URLEncoder.encode(queryString));

        String requestUrl = Utils.ReplaceTokens(MOVIE_SEARCH_URL, tokens);

        final RESTHandler handler = new RESTHandler();

        handler.onSuccess = new RESTHandler.RESTCallback()
        {
            @Override
            public void callback(RESTHandler.RESTResponse result)
            {
                try
                {
                    Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                            String.format("Got success from RESTHandler, payload: %d bytes",
                                    result.responseBody.length()));

                    ArrayList<TMDBMovie> resultObj;

                    Type collectionType = new TypeToken<ArrayList<TMDBMovie>>()
                    {
                    }.getType();
                    resultObj = new Gson().fromJson(result.responseBody, collectionType);

                    ArrayList<String> movieList = new ArrayList<String>();

                    for (TMDBMovie movie : resultObj)
                    {
                        movieList.add(movie.name);
                    }

                    Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                            String.format("Calling Autocomplete success callback with %d movies", movieList.size()));
                    onSuccess.callback(movieList);
                }
                catch (Exception ex)
                {
                    if (onError != null)
                    {
                        RESTHandler.RESTResponse errorResult = handler.new RESTResponse();

                        errorResult.exceptionMessage = ex.getMessage();
                        errorResult.isError = true;
                        errorResult.statusMessage = "TMDBAPI.AutocompleteMovies: Exception while handling service response";

                        onError.callback(errorResult);
                    }
                }
            }
        };

        handler.onError = onError;

        Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format("Calling TMDB API with request URL: %s", requestUrl));

        handler.execute(requestUrl);
    }

    public interface SearchMovieCallback
    {
        public void callback(ArrayList<TMDBMovie> result);
    }

    public void SearchMovie(String queryString, final SearchMovieCallback onSuccess,
            final RESTHandler.RESTCallback onError)
    {
        if (onSuccess == null) { return; }

        final String LOGTAG = "SearchMovie";

        Hashtable<String, String> tokens = new Hashtable<String, String>();

        tokens.put(APIKEY_TOKEN, TMDB_API_KEY);
        tokens.put(QUERY_TOKEN, URLEncoder.encode(queryString));

        String requestUrl = Utils.ReplaceTokens(MOVIE_SEARCH_URL, tokens);

        final RESTHandler handler = new RESTHandler();

        handler.onSuccess = new RESTHandler.RESTCallback()
        {
            @Override
            public void callback(RESTHandler.RESTResponse result)
            {
                try
                {
                    Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                            String.format("Got success from RESTHandler, payload: %d bytes",
                                    result.responseBody.length()));

                    ArrayList<TMDBMovie> resultObj;

                    if (result.responseBody.equals(SEARCH_MOVIE_NO_RESULT_TOKEN))
                    {
                        resultObj = new ArrayList<TMDBMovie>();
                    }
                    else
                    {
                        Type collectionType = new TypeToken<ArrayList<TMDBMovie>>()
                        {
                        }.getType();
                        resultObj = new Gson().fromJson(result.responseBody, collectionType);
                    }

                    Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                            String.format("Calling SearchMovie success callback with %d movies", resultObj.size()));
                    onSuccess.callback(resultObj);
                }
                catch (Exception ex)
                {
                    if (onError != null)
                    {
                        RESTHandler.RESTResponse errorResult = handler.new RESTResponse();

                        errorResult.exceptionMessage = ex.getMessage();
                        errorResult.isError = true;
                        errorResult.statusMessage = "TMDBAPI.SearchMovie: Exception while handling service response";

                        onError.callback(errorResult);
                    }
                }

            }
        };

        handler.onError = onError;

        Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format("Calling TMDB API with request URL: %s", requestUrl));

        handler.execute(requestUrl);
    }

    public class TMDBImageData
    {
        public String id;
        public String type;
        public String size;
        public int    height, width;
        public String url;
    }

    public class TMDBImage
    {
        public TMDBImageData image;
    }

    public class TMDBMovie
    {
        public String      id;
        public String      name;
        public TMDBImage[] posters;

        public String getThumbnailURL()
        {
            if (posters == null) { return null; }

            String retval = null;
            int minWidth = Integer.MAX_VALUE;

            for (TMDBImage checkImage : posters)
            {
                if (checkImage.image != null && checkImage.image.width < minWidth)
                {
                    minWidth = checkImage.image.width;
                    retval = checkImage.image.url;
                }
            }

            return retval;
        }
    }
}
