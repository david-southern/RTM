package com.spekisoftware.RTM;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import android.database.DataSetObserver;
import android.graphics.Bitmap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MovieListProxy
{
    private static final int                  MOVIE_UNDO_THRESHOLD_MILLIS = 10000;

    private static ArrayList<DataSetObserver> observers                   = new ArrayList<DataSetObserver>();

    public static synchronized void RegisterChangeObserver(DataSetObserver observer)
    {
        if (observers.contains(observer)) { return; }

        observers.add(observer);
    }

    public static synchronized void UnRegisterChangeObserver(DataSetObserver observer)
    {
        if (observers.contains(observer))
        {
            observers.remove(observer);
        }
    }

    private static synchronized void notifyChangeObservers()
    {
        for (DataSetObserver notifyObserver : observers)
        {
            notifyObserver.onChanged();
        }
    }

    public static final String SOURCE_MANUAL = "Manual";
    public static final String SOURCE_REDBOX = "Redbox";

    public static synchronized ArrayList<Movie> LoadMovieList()
    {
        ArrayList<Movie> retval;

        String moviesJSON = PrefsProxy.getMovieList();
        Logger.Log(Logger.LOG_LEVEL_DEBUG, "MovieList.Load",
                String.format("Loaded %d bytes of movie list", moviesJSON.length()));

        Type collectionType = new TypeToken<ArrayList<Movie>>()
        {
        }.getType();
        retval = new Gson().fromJson(moviesJSON, collectionType);

        ArrayList<Movie> expiredUndoList = new ArrayList<Movie>();

        for (Movie checkUndoExpired : retval)
        {
            if (checkUndoExpired.getUndoRentalTimestamp() > 0
                    && System.currentTimeMillis() > checkUndoExpired.getUndoRentalTimestamp())
            {
                Logger.Log(Logger.LOG_LEVEL_INFO, "MovieProxy.SaveMovieList", String.format(
                        "Returned movie %s has passed he undoExpire threshold, adding it to the remove list",
                        checkUndoExpired.getName()));
                expiredUndoList.add(checkUndoExpired);
            }
        }

        for (Movie delMovie : expiredUndoList)
        {
            delMovie.deleteImage();
            retval.remove(delMovie);
            Logger.Log(
                    Logger.LOG_LEVEL_INFO,
                    "MovieProxy.LoadMovieList",
                    String.format("Removing returned movie %s from the list, undo period has expired",
                            delMovie.getName()));
        }

        if (expiredUndoList.size() > 0)
        {
            SaveMovieList(retval);
        }

        return retval;
    }

    private static class MovieComparator implements Comparator<Movie>
    {
        @Override
        public int compare(Movie lhs, Movie rhs)
        {
            return lhs.getRentalDate().compareTo(rhs.getRentalDate());
        }
    }

    public static synchronized void SaveMovieList(ArrayList<Movie> movies)
    {
        Collections.sort(movies, new MovieComparator());

        ArrayList<Movie> expiredUndoList = new ArrayList<Movie>();

        for (Movie checkUndoExpired : movies)
        {
            if (checkUndoExpired.getUndoRentalTimestamp() > 0
                    && System.currentTimeMillis() > checkUndoExpired.getUndoRentalTimestamp())
            {
                Logger.Log(Logger.LOG_LEVEL_INFO, "MovieProxy.SaveMovieList", String.format(
                        "Returned movie %s has passed he undoExpire threshold, adding it to the remove list",
                        checkUndoExpired.getName()));
                expiredUndoList.add(checkUndoExpired);
            }
        }

        for (Movie delMovie : expiredUndoList)
        {
            delMovie.deleteImage();
            movies.remove(delMovie);
            Logger.Log(
                    Logger.LOG_LEVEL_INFO,
                    "MovieProxy.SaveMovieList",
                    String.format("Removing returned movie %s from the list, undo period has expired",
                            delMovie.getName()));
        }

        for (Movie thisMovie : movies)
        {
            thisMovie.saveImage();
        }

        String moviesJSON = new Gson().toJson(movies);

        Logger.Log(Logger.LOG_LEVEL_DEBUG, "MovieList/Save",
                String.format("Saving %d bytes of movie list", moviesJSON.length()));

        PrefsProxy.setMovieList(moviesJSON);
    }

    public static synchronized void RentMovie(String movieName, Date rentalDate, String rentalSource)
    {
        Logger.Log(Logger.LOG_LEVEL_INFO, "MovieList.Rent", String.format("Renting Movie %s", movieName));

        ArrayList<Movie> movies = LoadMovieList();

        for (Movie checkMovie : movies)
        {
            if (checkMovie.getName().equals(movieName) && checkMovie.getRentalDate().getTime() == rentalDate.getTime()) { return; }
        }

        final Movie newMovie = new Movie(movieName, rentalDate, rentalSource);

        Logger.Log(
                Logger.LOG_LEVEL_INFO,
                "MovieList.Rent",
                String.format("Movie %s rented at %s", movieName,
                        SimpleDateFormat.getDateTimeInstance().format(newMovie.getRentalDate().getTime())));

        int nextMovieId = PrefsProxy.getNextMovieID();

        newMovie.setId(nextMovieId++);

        movies.add(newMovie);

        SaveMovieList(movies);

        PrefsProxy.setNextMovieID(nextMovieId);

        TMDBAPI api = new TMDBAPI();

        api.SearchMovie(movieName, new TMDBAPI.SearchMovieCallback()
        {
            @Override
            public void callback(ArrayList<TMDBAPI.TMDBMovie> result)
            {
                if (result.size() > 0)
                {
                    final String imageURL = result.get(0).getThumbnailURL();

                    if (imageURL != null && imageURL.trim().length() > 0)
                    {

                        Logger.Log(Logger.LOG_LEVEL_DEBUG, "MovieList.Rent",
                                String.format("Downloading movie image: %s", imageURL));

                        ImageDownloader downer = new ImageDownloader();

                        downer.onSuccess = new ImageDownloader.SuccessCallback()
                        {
                            @Override
                            public void callback(Bitmap resultImage)
                            {
                                ArrayList<Movie> movies = LoadMovieList();

                                for (Movie setMovie : movies)
                                {
                                    if (setMovie.getId() != newMovie.getId())
                                    {
                                        continue;
                                    }

                                    setMovie.setImageURL(imageURL);
                                    setMovie.setImage(resultImage);

                                    Logger.Log(Logger.LOG_LEVEL_INFO, "RentMovie", String.format(
                                            "Setting movie %s image [%d x %d] URL to: %s", newMovie.getName(),
                                            resultImage.getWidth(), resultImage.getHeight(), setMovie.getImageURL()));

                                    break;
                                }

                                SaveMovieList(movies);

                                notifyChangeObservers();
                            }
                        };

                        downer.execute(imageURL);
                    }
                }
            }
        }, null);
    }

    public static synchronized void ReturnMovie(Movie movie)
    {
        if (movie == null) { return; }

        Logger.Log(Logger.LOG_LEVEL_INFO, "MovieList.Return", String.format("Returning Movie %s", movie.getName()));

        ArrayList<Movie> movies = LoadMovieList();

        int movieIndex = 0;

        for (Movie checkMovie : movies)
        {
            if (checkMovie.getId() == movie.getId())
            {
                movie = checkMovie;
                break;
            }
            movieIndex++;
        }

        if (movieIndex == movies.size())
        {
            Logger.Log(Logger.LOG_LEVEL_ERROR, "Movie.Rent",
                    String.format("Error. movie %s was not found in movie list", movie.getName()));
        }
        else
        {
            if (movie.getUndoRentalTimestamp() > 0)
            {
                Logger.Log(Logger.LOG_LEVEL_INFO, "MovieList.Rent",
                        String.format("UNDO: Return of movie %s", movie.getName()));
                movie.setUndoRentalTimestamp(0);
            }
            else
            {
                Logger.Log(Logger.LOG_LEVEL_INFO, "MovieList.Rent", String.format("Movie %s returned", movie.getName()));
                movie.setUndoRentalTimestamp(System.currentTimeMillis() + MOVIE_UNDO_THRESHOLD_MILLIS);
            }
        }

        SaveMovieList(movies);
    }

    public static void AddTestMovieData()
    {
        Calendar testDate = Calendar.getInstance();
        RentMovie("The Big Lebowski", testDate.getTime(), "Manual");
        testDate.add(Calendar.DAY_OF_YEAR, -1);
        RentMovie("Intolerable Cruelty", testDate.getTime(), "Manual");
        testDate.add(Calendar.DAY_OF_YEAR, -1);
        RentMovie("Ladykillers", testDate.getTime(), "Manual");
        testDate.add(Calendar.DAY_OF_YEAR, -1);
        RentMovie("True Grit", testDate.getTime(), "Manual");
        testDate.add(Calendar.DAY_OF_YEAR, -1);
        RentMovie("Fargo", testDate.getTime(), "Manual");
        testDate.add(Calendar.DAY_OF_YEAR, -1);
        RentMovie("Burn After Reading", testDate.getTime(), "Manual");
        testDate.add(Calendar.DAY_OF_YEAR, -1);
        RentMovie("Raising Arizona", testDate.getTime(), "Manual");
    }

    public static Movie findMovieById(int movieID)
    {
        Logger.Log(Logger.LOG_LEVEL_INFO, "MovieList.FindMovie", String.format("Finding Movie %d", movieID));

        ArrayList<Movie> movies = LoadMovieList();

        for (Movie checkMovie : movies)
        {
            if (checkMovie.getId() == movieID) { return checkMovie; }
        }

        return null;
    }

    public static void ClearMovieData()
    {
        ArrayList<Movie> movies = LoadMovieList();

        for (Movie returnMovie : movies)
        {
            ReturnMovie(returnMovie);
        }
    }

    public static Date convertRedboxDate(String redboxDate)
    {
        if (redboxDate == null || redboxDate.trim().length() < 1 || redboxDate.trim().equals("--")) { return null; }

        // In my browser, redbox shows dates as "MM/DD/YY". Let's hope this
        // works for other locales.

        SimpleDateFormat parser = new SimpleDateFormat("MM/dd/yy");

        try
        {
            return parser.parse(redboxDate);
        }
        catch (java.text.ParseException ex)
        {
            Logger.Log(Logger.LOG_LEVEL_ERROR, "MovieProxList",
                    String.format("Error parsing redbox date: %s", redboxDate));
            return null;
        }
    }

    private static String prettyDate(Date rawDate)
    {
        if (rawDate == null) { return "<null>"; }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        return formatter.format(rawDate);
    }

    public static final int RENTAL_STATUS_NEW_RENTAL = 1;
    public static final int RENTAL_STATUS_NEW_RETURN = 2;
    public static final int RENTAL_STATUS_NO_ACTION  = 3;

    public static int RedboxScrapeHandleResults(String movieName, Date rentalDate, Date returnDate, String returnStatus)
    {
        final String LOGTAG = "RedboxScrapeHandleResults";
        int retval = RENTAL_STATUS_NO_ACTION;

        Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format(
                "Handling result: %s, Source: %s, Rented: %s, Returned: %s", movieName, SOURCE_REDBOX,
                prettyDate(rentalDate), prettyDate(returnDate)));

        ArrayList<Movie> movies = LoadMovieList();
        ArrayList<Movie> removeList = new ArrayList<Movie>();
        boolean addMovie = true;

        for (Movie checkMovie : movies)
        {
            Logger.Log(
                    Logger.LOG_LEVEL_INFO,
                    LOGTAG,
                    String.format("Considering movie %s, Source: %s, Rented: %s", checkMovie.getName(),
                            checkMovie.getRentalSource(), prettyDate(checkMovie.getRentalDate())));

            if (MovieNameMatch(checkMovie.getName(), movieName))
            {
                addMovie = false;

                if (returnDate != null)
                {
                    Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                            "Found a movie name match, considering whether to auto-remove the movie");
                    if (checkMovie.getRentalSource() == SOURCE_REDBOX)
                    {
                        // If the scraper added it then go ahead and remove it
                        Logger.Log(Logger.LOG_LEVEL_INFO, "RedboxScrapeHandleResults",
                                "Scraped Return Date is set, and movie originally came from Redbox, so returning it");
                        removeList.add(checkMovie);
                    }
                    else
                    {
                        // If the user added this movie manually, then check and see if the manual rental date
                        // is newer than the redbox return date. If this is the case then perhaps they returned
                        // the movie to redbox and later rented the same movie again on a different account? In
                        // and event, only auto-remove if the redbox entry looks like it was the same as the
                        // manual entry.

                        if (checkMovie.getRentalDate().after(returnDate))
                        {
                            // The manual rental was added after redbox showed it returned, don't auto-remove it
                            Logger.Log(
                                    Logger.LOG_LEVEL_INFO,
                                    LOGTAG,
                                    "Return Date is set, but movie did not come Redbox, and the movie's rental date is newer than the scraped return date, so leaving the movie alone");
                            continue;
                        }

                        // Perhaps we should check if the manual rental date is 'close' to the redbox rental date? For
                        // now just remove it
                        removeList.add(checkMovie);
                    }
                }
                else
                {
                    Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                            "Found a movie name match, updating rental date in case the user entered it on the wrong date");
                    checkMovie.setRentalDate(rentalDate);
                }
            }
        }

        for (Movie removeMovie : removeList)
        {
            ReturnMovie(removeMovie);
            retval = RENTAL_STATUS_NEW_RETURN;
        }

        if (addMovie)
        {
            if (returnDate == null)
            {
                Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                        "No matching movie found in the list, and return date is null, so adding movie to rented list");
                RentMovie(movieName, rentalDate, SOURCE_REDBOX);
                retval = RENTAL_STATUS_NEW_RENTAL;
            }
            else
            {
                Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                        "No matching movie found in the list, but return date is null, so ignoring this scrape result");
            }
        }

        return retval;
    }

    private static boolean MovieNameMatch(String firstName, String secondName)
    {
        return firstName.equals(secondName);
    }
}
