package com.spekisoftware.RTM;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ScrapeService extends Service
{
    private static Object serviceLock = new Object();
    private static boolean isScraping = false;
    
    @Override
    public void onCreate()
    {
        Logger.Log(Logger.LOG_LEVEL_INFO, "ScrapeService.Create", "Scrape service created");
        super.onCreate();
    }

    @Override
    public void onDestroy()
    {
        Logger.Log(Logger.LOG_LEVEL_INFO, "ScrapeService.Destroy", "Scrape service destroyed");
        super.onDestroy();
        
        synchronized(serviceLock)
        {
            isScraping = false;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        synchronized(serviceLock)
        {
            if(isScraping)
            {
                // Schedule the alarm to fire again in 5 seconds.  We don't want two scrapes happening
                // at once, however suppose they mis-entered thier Redbox creds  and clicked Ok.  This will
                // start a scrape.  If they then realize that they made a mistake, and go fix it, we need
                // to do another scrape with the fixed creds.  If we simply returned here then the second
                // scrape would never happen.  Instead set the scrape alarm again.  With the true parameter,
                // this will cause the alarm to fire in 5 seconds.  We will keep calling the service until
                // the first scrape is finished, at which point we will do the second scrape.
                Logger.Log(Logger.LOG_LEVEL_INFO, "ScrapeService", "ScrapeService is already running, will re-schedule a scrape alarm");
                ChangePrefs.SetScrapeAlarm(true);
                return Service.START_STICKY;
            }
            
            isScraping = true;
        }
        
        String LOGTAG = "ScrapeService.Start";
        
        Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, "ScrapeService started");
        
        super.onStartCommand(intent, flags, startId);     
        
        ScrapeAsync scrapeTask = new ScrapeAsync();
        scrapeTask.parentService = this;
        scrapeTask.execute();
        
        Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, "Returning START_STICKY from ScrapeService");
        
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // This service is not bound
        return null;
    }

}
