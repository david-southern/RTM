package com.spekisoftware.RTM;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TimePicker;

public class ChangePrefs extends CustomTitleActivity
{

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        final ChangePrefs parent = this;

        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.change_prefs_view);
        Spinner notifyPeriodSpinner = (Spinner)findViewById(R.id.NotificationPeriodSelector);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.NotificationPeriodItems,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        notifyPeriodSpinner.setAdapter(adapter);

        Button okButton = (Button)findViewById(R.id.OkButton);
        okButton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                parent.OkClick();
            }

        });
    }

    @Override
    protected void onResume()
    {
        // TODO Auto-generated method stub
        super.onResume();

        int notifyPeriod = PrefsProxy.getNotifyPeriod();
        Spinner notifyPeriodSpinner = (Spinner)findViewById(R.id.NotificationPeriodSelector);
        notifyPeriodSpinner.setSelection(notifyPeriod - 1);

        int notifyTime = PrefsProxy.getNotifyTime();
        TimePicker notifyTimePicker = (TimePicker)findViewById(R.id.NotificationTimePicker);
        notifyTimePicker.setCurrentHour(notifyTime / 3600);
        notifyTimePicker.setCurrentMinute((notifyTime % 3600) / 60);
    }

    private void OkClick()
    {
        Spinner notifyPeriodSpinner = (Spinner)findViewById(R.id.NotificationPeriodSelector);
        int notifyPeriod = notifyPeriodSpinner.getSelectedItemPosition() + 1;
        PrefsProxy.setNotifyPeriod(notifyPeriod);

        TimePicker notifyTimePicker = (TimePicker)findViewById(R.id.NotificationTimePicker);
        int notifyTime = notifyTimePicker.getCurrentHour() * 3600 + notifyTimePicker.getCurrentMinute() * 60;
        PrefsProxy.setNotifyTime(notifyTime);

        SetNotificationAlarm();

        // If we have just changed the notification time, make the scraper fire immediately. Reason: For the
        // first-time downloader, they will expect to see their scraped redbox results right away. Don't make
        // them wait until tomorrow.
        // Second Reason: The scraper fires a random time up to 6 hours before the
        // notification time. If we happen to have set up the new notification time during that 6 hour period then we
        // might 'skip' the scrape task until the next day if we don't scrape immediately, and that would make a bad
        // impression.
        SetScrapeAlarm(true);

        finish();
    }

    public static void SetServiceAlarms()
    {
        SetNotificationAlarm();
        SetScrapeAlarm(false);
    }

    public static void SetNotificationAlarm()
    {
        SetNotificationAlarm(0);
    }

    public static void SetNotificationAlarm(int snoozeMinutes)
    {
        Intent intent = new Intent(CheckNotificationsReceiver.ACTION_CHECK_NOTIFICATIONS);
        PendingIntent pendIntent = PendingIntent.getBroadcast(ReminderApplication.getContext(), 0, intent, 0);

        AlarmManager mgr = (AlarmManager)ReminderApplication.getContext().getSystemService(Context.ALARM_SERVICE);

        Calendar noteTime = Calendar.getInstance();

        if (snoozeMinutes == 0)
        {
            int notifyTime = PrefsProxy.getNotifyTime();

            noteTime.set(Calendar.HOUR_OF_DAY, 0);
            noteTime.set(Calendar.MINUTE, 0);
            noteTime.set(Calendar.SECOND, 0);
            noteTime.set(Calendar.MILLISECOND, 0);

            noteTime.add(Calendar.SECOND, notifyTime);

            // Bump the time check forward 5 seconds, just in case we call this from the notification alarm
            // event itself, and we get here fast enough that the millis haven't advanced.
            if (noteTime.getTimeInMillis() < (System.currentTimeMillis() + 5000))
            {
                noteTime.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
        else            
        {
            noteTime.add(Calendar.MINUTE, snoozeMinutes);
        }

        mgr.set(AlarmManager.RTC_WAKEUP, noteTime.getTimeInMillis(), pendIntent);

        Logger.Log(Logger.LOG_LEVEL_INFO, "ServiceAlarm", String.format(
                "Set notification service alarm for %d(%s) (time now: %d(%s))", noteTime.getTimeInMillis(), DateUtils
                        .formatDateTime(ReminderApplication.getContext(), noteTime.getTimeInMillis(),
                                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME), System.currentTimeMillis(),
                DateUtils.formatDateTime(ReminderApplication.getContext(), System.currentTimeMillis(),
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME)));
    }

    // Scrape randomly over a 6 hour period
    private static final int SCRAPER_RANDOM_PERIOD_SECONDS        = 6 * 3600;

    // Always schedule the scrape for at least 30 minutes before the notification, to make sure it is completed when the
    // notification fires
    private static final int SCRAPER_RANDOM_MIN_THRESHOLD_SECONDS = 1800;

    public static void SetScrapeAlarm(boolean immediate)
    {
        String LOGTAG = "SetScrapeAlarm";

        if (!PrefsProxy.hasRedboxCredentials())
        {
            Logger.Log(Logger.LOG_LEVEL_WARN, LOGTAG,
                    "There are no Redbox login credentials stored, no scraping alarm will be set");
            return;
        }

        int notifyTime = PrefsProxy.getNotifyTime();

        Intent intent = new Intent(CheckNotificationsReceiver.ACTION_SCRAPE_MOVIES);
        PendingIntent pendIntent = PendingIntent.getBroadcast(ReminderApplication.getContext(), 0, intent, 0);

        AlarmManager mgr = (AlarmManager)ReminderApplication.getContext().getSystemService(Context.ALARM_SERVICE);

        Calendar noteTime = Calendar.getInstance();

        if (immediate)
        {
            noteTime.add(Calendar.SECOND, 5);
        }
        else
        {
            noteTime.set(Calendar.HOUR_OF_DAY, 0);
            noteTime.set(Calendar.MINUTE, 0);
            noteTime.set(Calendar.SECOND, 0);
            noteTime.set(Calendar.MILLISECOND, 0);

            noteTime.add(Calendar.SECOND, notifyTime);

            // If we ever do sell lots of reminder apps, we don't want to be hitting Redbox all at the same time.
            // For this reason, spread the scrapes out randomly over the 6 hours before the notification
            int randomOffset = -(int)(Math.random() * SCRAPER_RANDOM_PERIOD_SECONDS + SCRAPER_RANDOM_MIN_THRESHOLD_SECONDS);
            Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format("Setting scrape offset to %d", randomOffset));
            noteTime.add(Calendar.SECOND, randomOffset);

            // Bump the time check forward 5 seconds, just in case we call this from the notification alarm
            // event itself, and we get here fast enough that the millis haven't advanced.
            if (noteTime.getTimeInMillis() < (System.currentTimeMillis() + 5000))
            {
                noteTime.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        mgr.set(AlarmManager.RTC_WAKEUP, noteTime.getTimeInMillis(), pendIntent);

        Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format(
                "Set scrape service alarm for %d(%s) (time now: %d(%s))", noteTime.getTimeInMillis(), DateUtils
                        .formatDateTime(ReminderApplication.getContext(), noteTime.getTimeInMillis(),
                                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME), System.currentTimeMillis(),
                DateUtils.formatDateTime(ReminderApplication.getContext(), System.currentTimeMillis(),
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME)));
    }

}
