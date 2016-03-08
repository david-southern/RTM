package com.spekisoftware.RTM;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

public class MovieListActivity extends CustomTitleActivity
{
    public static final String FROM_NOTIFICATION = "FromNotification";

    MovieAdapter               movieAdapter      = null;
    BroadcastReceiver          updateReceiver    = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.movie_list_view);

        final MovieListActivity activityContext = this;

        Button rentButton = (Button)findViewById(R.id.newRentalButton);

        rentButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent rentIntent = new Intent(activityContext, NewRental.class);
                startActivity(rentIntent);
            }
        });

        Button changePrefsButton = (Button)findViewById(R.id.changePrefsButton);

        changePrefsButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent prefsIntent = new Intent(activityContext, ChangePrefs.class);
                startActivity(prefsIntent);
            }
        });

        Button redboxButton = (Button)findViewById(R.id.gotoRedboxButton);

        redboxButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent redboxIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.redbox.com/"));
                startActivity(redboxIntent);
            }
        });

        movieAdapter = new MovieAdapter(this);

        final View emptyView = findViewById(R.id.emptyList);
        final ListView lv = (ListView)findViewById(R.id.movieListView);

        lv.setAdapter(movieAdapter);

        movieAdapter.registerDataSetObserver(new DataSetObserver()
        {
            @Override
            public void onChanged()
            {
                Logger.Log(Logger.LOG_LEVEL_INFO, "MovieList",
                        "Adapter DataSetObsever has fired onChanged.  Invalidating ListView.");
                super.onChanged();
                if (movieAdapter.getCount() < 1)
                {
                    emptyView.setVisibility(View.VISIBLE);
                    lv.setVisibility(View.GONE);
                }
                else
                {
                    emptyView.setVisibility(View.GONE);
                    lv.setVisibility(View.VISIBLE);
                    lv.invalidate();
                }
            }

        });

        Button updateLoginInfo = (Button)findViewById(R.id.UpdateLoginInfo);

        updateLoginInfo.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                Intent redboxIntent = new Intent(activityContext, RedboxCredentials.class);
                startActivity(redboxIntent);
            }

        });

        Button updateStatusButton = (Button)findViewById(R.id.UpdateStatusButton);
        updateStatusButton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                PrefsProxy.setLastScrapeStatus(null);
                ChangePrefs.SetScrapeAlarm(true);
                updateScrapeStatus();
            }
        });

    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        movieAdapter.UnRegisterMovieObserver();
    }

    private static final int DIALOG_SPLASH_SCREEN = 1;
    private static final int DIALOG_NOTIFICATION  = 2;

    @Override
    protected Dialog onCreateDialog(int id)
    {
        super.onCreateDialog(id);

        if (id == DIALOG_SPLASH_SCREEN)
        {

            String infoText = getString(R.string.ScrapeInfoShortBlurb);
            WebView wv = new WebView(getBaseContext());
            wv.loadData(infoText, "text/html", null);
            wv.setBackgroundColor(Color.TRANSPARENT);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(wv);
            builder.setIcon(R.drawable.ic_menu_help);
            builder.setTitle(R.string.ScrapeInfoTitle);
            builder.setNeutralButton(R.string.ScrapeInfoOkButton, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    PrefsProxy.setSeenSplashScreen(true);
                    dialog.cancel();
                }
            });

            return builder.create();
        }

        if (id == DIALOG_NOTIFICATION)
        {
            LayoutInflater inflater;
            inflater = LayoutInflater.from(this);
            LinearLayout snoozeView = (LinearLayout)inflater.inflate(R.layout.notification_snoozer, null);

            final Spinner snoozeSpinner = (Spinner)snoozeView.findViewById(R.id.SnoozerSpinner);
            snoozeSpinner.setSelection(3);

            TextView snoozeMessage = (TextView)snoozeView.findViewById(R.id.SnoozerMessage);

            snoozeMessage.setText(R.string.NotificationSnoozerMessage);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(snoozeView);
            builder.setIcon(R.drawable.rental_button);
            builder.setTitle(R.string.NotificationSnoozerTitle);
            builder.setNeutralButton(R.string.NotificationSnoozerOk, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    PrefsProxy.setSeenSplashScreen(true);
                    dialog.cancel();
                }
            });
            builder.setNegativeButton(R.string.NotificationSnoozerSnooze, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    String snoozeString = (String)snoozeSpinner.getSelectedItem();

                    int snoozeAmount = Integer.parseInt((String)snoozeString.subSequence(0, snoozeString.indexOf(' ')));
                    int snoozePeriod = 1;

                    if (snoozeString.toLowerCase().contains("hour"))
                    {
                        snoozePeriod = 60;
                    }

                    int snoozeMinutes = snoozeAmount * snoozePeriod;

                    Logger.Log(Logger.LOG_LEVEL_INFO, "SnoozeDialog",
                            String.format("Snoozing notification alarm for %d minutes", snoozeMinutes));
                    ChangePrefs.SetNotificationAlarm(snoozeMinutes);
                    Logger.Log(Logger.LOG_LEVEL_WARN, "MovieList",
                            String.format("Snoozing MovieList from activity: %d", MovieListActivity.this.hashCode()));
                    notificationSnoozed = true;
                    MovieListActivity.this.finish();
                }
            });

            return builder.create();
        }

        return null;
    }

    private static boolean notificationSnoozed = false;
    private static long    lastSnoozeTS        = -1;

    protected void onResume()
    {
        super.onResume();

        Logger.Log(Logger.LOG_LEVEL_WARN, "MovieList",
                String.format("Resuming MovieList from activity: %d", this.hashCode()));

        if (notificationSnoozed)
        {
            Logger.Log(Logger.LOG_LEVEL_INFO, "MovieList", "Saw notificationSnoozed, finishing activity");
            MovieListActivity.this.finish();
        }

        notificationSnoozed = false;

        final MovieListActivity parent = this;

        updateReceiver = new BroadcastReceiver()
        {

            @Override
            public void onReceive(Context context, Intent intent)
            {
                Logger.Log(Logger.LOG_LEVEL_INFO, "MovieList", "Received UPDATE_SCRAPE_STATUS Intent");
                parent.updateScrapeStatus();
            }

        };

        this.registerReceiver(updateReceiver, new IntentFilter(CheckNotificationsReceiver.ACTION_UPDATE_SCRAPE_STATUS));

        movieAdapter.updateMovieList();

        updateScrapeStatus();

        Intent startingIntent = getIntent();

        Logger.Log(Logger.LOG_LEVEL_INFO, "MovieList",
                "Checking to see if intent has FromNotification: %s, FromNotification value: %d",
                startingIntent.hasExtra(FROM_NOTIFICATION) ? "YES" : "NO",
                startingIntent.getLongExtra(FROM_NOTIFICATION, 0));

        if (startingIntent.hasExtra(FROM_NOTIFICATION))
        {
            long thisSnoozeTS = startingIntent.getLongExtra(FROM_NOTIFICATION, 0);

            if (lastSnoozeTS != thisSnoozeTS)
            {
                lastSnoozeTS = thisSnoozeTS;
                showDialog(DIALOG_NOTIFICATION);
            }
            return;
        }

        if (!PrefsProxy.getSeenSplashScreen())
        {
            showDialog(DIALOG_SPLASH_SCREEN);
            return;
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if (updateReceiver != null)
        {
            this.unregisterReceiver(updateReceiver);
        }
    }

    private void updateScrapeStatus()
    {
        TextView updateStatus = (TextView)findViewById(R.id.RedboxUpdateStatus);

        String statusMessage = getString(R.string.AutoScrapeMessage);

        Button updateStatusButton = (Button)findViewById(R.id.UpdateStatusButton);
        Button updateLoginInfoButton = (Button)findViewById(R.id.UpdateLoginInfo);

        if (PrefsProxy.hasRedboxCredentials())
        {
            updateStatusButton.setVisibility(View.VISIBLE);

            Calendar lastScrapeTime = PrefsProxy.getLastScrapeTime();
            String lastScrapeStatus = PrefsProxy.getLastScrapeStatus();

            if (lastScrapeStatus != null)
            {
                statusMessage = String.format("%s: %s",
                        SimpleDateFormat.getDateTimeInstance().format(lastScrapeTime.getTime()), lastScrapeStatus);
            }
            else
            {
                statusMessage = getString(R.string.AutoScrapePending);
            }
            updateLoginInfoButton.setText(R.string.UpdateLoginInfo);
        }
        else
        {
            updateStatusButton.setVisibility(View.GONE);
            updateLoginInfoButton.setText(R.string.SetupLoginInfo);
        }

        updateStatus.setText(statusMessage);
        movieAdapter.updateMovieList();
    }
}