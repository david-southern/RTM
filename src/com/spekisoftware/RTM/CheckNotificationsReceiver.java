package com.spekisoftware.RTM;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CheckNotificationsReceiver extends BroadcastReceiver
{
    public static final String ACTION_CHECK_NOTIFICATIONS = "com.spekisoftware.CheckNotifications";
    public static final String ACTION_SCRAPE_MOVIES       = "com.spekisoftware.ScrapeMovies";
    public static final String ACTION_UPDATE_SCRAPE_STATUS       = "com.spekisoftware.UpdateScrapeStatus";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Logger.Log(Logger.LOG_LEVEL_INFO, "CheckNotification", String.format("Received check service broadcast for: %s", intent.getAction()));

        if (intent.getAction().equals(ACTION_CHECK_NOTIFICATIONS))
        {
            Intent serviceIntent = new Intent(context, NotificationService.class);
            context.startService(serviceIntent);
        }

        if (intent.getAction().equals(ACTION_SCRAPE_MOVIES))
        {
            Intent serviceIntent = new Intent(context, ScrapeService.class);
            context.startService(serviceIntent);
        }
        
        // Whenever we dispatch a service, re-schedule the alarms again to make sure we occur every day.
        // This will cause that we re-schedule one alarm each time, but that should be ok, as the AlarmManager
        // automatically cancels any pending alarm when it schedules a new alarm
        ChangePrefs.SetServiceAlarms();
    }

}
