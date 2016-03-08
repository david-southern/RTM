package com.spekisoftware.RTM;

import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsProxy
{
    public static final String  MOVIE_ID_KEY             = "MovieID.Key";

    private static final String MOVIE_PREFS_NAME         = "MovieList.prefs";

    private static final String PREFS_MOVIE_LIST         = "MovieList.JSON";
    private static final String PREFS_NEXT_ID            = "NextMovieID";

    private static final String PREFS_REDBOX_CREDENTIALS = "MovieList.Signature";

    private static final String PREFS_NOTIFY_PERIOD      = "NotifyPeriod";
    private static final String PREFS_NOTIFY_TIME        = "NotifyTime";

    private static final String PREFS_LAST_SCRAPE_TIME   = "LastScrapeTime";
    private static final String PREFS_LAST_SCRAPE_STATUS = "LastScrapeStatus";

    private static final String PREFS_SEEN_SPLASH_SCREEN = "SeenSplashScreen";

    private static final int    DEFAULT_NOTIFY_PERIOD    = 2;
    private static final int    DEFAULT_NOTIFY_TIME      = (int)(8 * 3600);

    private static SharedPreferences OpenPrefs()
    {
        Context appContext = ReminderApplication.getContext();
        SharedPreferences retval = appContext.getSharedPreferences(MOVIE_PREFS_NAME, Context.MODE_PRIVATE);
        return retval;
    }

    public static int getNotifyPeriod()
    {
        return OpenPrefs().getInt(PREFS_NOTIFY_PERIOD, DEFAULT_NOTIFY_PERIOD);
    }

    public static int getNotifyTime()
    {
        return OpenPrefs().getInt(PREFS_NOTIFY_TIME, DEFAULT_NOTIFY_TIME);
    }

    public static String getMovieList()
    {
        return OpenPrefs().getString(PREFS_MOVIE_LIST, "[]");
    }

    public static void setMovieList(String movieListJSON)
    {
        SharedPreferences prefs = OpenPrefs();

        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(PREFS_MOVIE_LIST, movieListJSON);
        editor.commit();

    }

    public static int getNextMovieID()
    {
        return OpenPrefs().getInt(PREFS_NEXT_ID, 1);
    }

    public static void setNextMovieID(int nextMovieId)
    {
        SharedPreferences.Editor editor = OpenPrefs().edit();
        editor.putInt(PREFS_NEXT_ID, nextMovieId);
        editor.commit();
    }

    public static void setNotifyPeriod(int notifyPeriod)
    {
        SharedPreferences.Editor editor = OpenPrefs().edit();
        editor.putInt(PREFS_NOTIFY_PERIOD, notifyPeriod);
        editor.commit();
    }

    public static void setNotifyTime(int notifyTime)
    {
        SharedPreferences.Editor editor = OpenPrefs().edit();
        editor.putInt(PREFS_NOTIFY_TIME, notifyTime);
        editor.commit();
    }

    public static boolean hasRedboxCredentials()
    {
        String encryptedCreds = OpenPrefs().getString(PREFS_REDBOX_CREDENTIALS, null);

        return encryptedCreds != null;
    }

    private static final String FIXED_CREDS_SALT = "I know this is not secure, just trying to dissuade casual hacking";

    public static void setRedboxCredentials(String userName, String password)
    {
        String credsString = null;

        if (userName != null && userName.trim().length() > 0 && password != null && password.trim().length() > 0)
        {
            // Encode to Base64 to avoid having to escape credential delimiter
            userName = Base64.encodeToString(userName.getBytes(), false);
            password = Base64.encodeToString(password.getBytes(), false);
            credsString = Utils.EncryptString(String.format("%s|%s|%s", FIXED_CREDS_SALT, userName, password));
        }

        SharedPreferences.Editor editor = OpenPrefs().edit();
        editor.putString(PREFS_REDBOX_CREDENTIALS, credsString);
        editor.commit();
    }

    public static String getRedboxUserName()
    {
        String LOGTAG = "getRedboxUserName";

        if (!hasRedboxCredentials()) { return null; }

        String credsString = Utils.DecryptString(OpenPrefs().getString(PREFS_REDBOX_CREDENTIALS, null));

        String[] parts = credsString.split("\\|");

        if (parts.length != 3)
        {
            Logger.Log(Logger.LOG_LEVEL_ERROR, LOGTAG,
                    String.format("Error, redbox credentials has wrong number of parts: %d", parts.length));
        }

        return new String(Base64.decodeFast(parts[1]));
    }

    public static String getRedboxPassword()
    {
        String LOGTAG = "getRedboxPassword";

        if (!hasRedboxCredentials()) { return null; }

        String credsString = Utils.DecryptString(OpenPrefs().getString(PREFS_REDBOX_CREDENTIALS, null));

        String[] parts = credsString.split("\\|");

        if (parts.length != 3)
        {
            Logger.Log(Logger.LOG_LEVEL_ERROR, LOGTAG,
                    String.format("Error, redbox credentials has wrong number of parts: %d", parts.length));
        }

        return new String(Base64.decodeFast(parts[2]));
    }

    public static Calendar getLastScrapeTime()
    {
        Calendar scrapeTime = Calendar.getInstance();
        scrapeTime.setTimeInMillis(OpenPrefs().getLong(PREFS_LAST_SCRAPE_TIME, scrapeTime.getTimeInMillis()));
        return scrapeTime;
    }

    public static String getLastScrapeStatus()
    {
        return OpenPrefs().getString(PREFS_LAST_SCRAPE_STATUS, null);
    }

    public static void setLastScrapeStatus(String lastScrapeStatus)
    {
        SharedPreferences.Editor editor = OpenPrefs().edit();
        editor.putLong(PREFS_LAST_SCRAPE_TIME, Calendar.getInstance().getTimeInMillis());
        editor.putString(PREFS_LAST_SCRAPE_STATUS, lastScrapeStatus);
        editor.commit();
    }

    public static boolean getSeenSplashScreen()
    {
        return OpenPrefs().getBoolean(PREFS_SEEN_SPLASH_SCREEN, false);
    }

    public static void setSeenSplashScreen(boolean status)
    {
        SharedPreferences.Editor editor = OpenPrefs().edit();
        editor.putBoolean(PREFS_SEEN_SPLASH_SCREEN, status);
        editor.commit();
    }
}
