package com.spekisoftware.RTM;

import java.util.ArrayList;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class NotificationService extends IntentService
{
    public NotificationService()
    {
        super("NotificationService");
    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns,
     * IntentService stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent)
    {
        Logger.Log(Logger.LOG_LEVEL_INFO, "NotificationService", "NotificationService has been called.");

        ArrayList<Movie> movies = MovieListProxy.LoadMovieList();

        if (movies.size() < 1)
        {
            Logger.Log(Logger.LOG_LEVEL_INFO, "NotificationService", "No movies are rented, exiting notification service");
            return;
        }

        if (movies.get(0).getAge() < PrefsProxy.getNotifyPeriod())
        {
            Logger.Log(Logger.LOG_LEVEL_INFO, "NotificationService", "Rented movies are younger than notify period, exiting notifiation service");
            return;
        }

        Logger.Log(Logger.LOG_LEVEL_INFO, "NotificationService", "Rented movies are older than notify period, sending reminder notification");
        NotificationManager mgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        Context noteContext = getApplicationContext();

        Notification note = new Notification(R.drawable.launcher_icon, noteContext.getString(R.string.app_name),
                System.currentTimeMillis());
        note.defaults |= Notification.DEFAULT_SOUND;
        note.flags |= Notification.FLAG_AUTO_CANCEL;

        Intent noteIntent = new Intent(this, MovieListActivity.class);
        noteIntent.putExtra(MovieListActivity.FROM_NOTIFICATION, System.currentTimeMillis());

        PendingIntent pendIntent = PendingIntent.getActivity(noteContext, 0, noteIntent, 0);

        note.setLatestEventInfo(noteContext, noteContext.getString(R.string.app_name), 
                noteContext.getString(R.string.NotificationMessage), pendIntent);

        mgr.notify(1, note);
    }
}
