package com.spekisoftware.RTM;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Looper;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ScrapeAsync extends AsyncTask<Void, Void, Void> implements Runnable
{
    private static final int SCRAPE_TIMEOUT_THRESHOLD_MILLIS = 60000;

    public Service           parentService                   = null;

    private Looper           parentLooper                    = null;

    private String           lastScrapeStatus                = null;

    private static int       scrapeCount                     = 1;

    @Override
    protected Void doInBackground(Void... params)
    {
        String LOGTAG = "ScrapeAsync";

        if (!PrefsProxy.hasRedboxCredentials())
        {
            Logger.Log(Logger.LOG_LEVEL_WARN, LOGTAG,
                    "Called ScrapeAsync, however Prefs has no Redbox credentials.  This should not happen.");
            return null;
        }

        Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, "ScrapeAsync has been started.");

        scrapeStatus = ScrapeStatusEnum.STARTED;

        // Start the background scraper
        new Thread(this, String.format("Scraper %d - %d", Thread.currentThread().getId(), scrapeCount++)).start();

        // Wait for the background scraper to finish
        long lastStatusChangeTicks = System.currentTimeMillis();
        ScrapeStatusEnum prevStatus = scrapeStatus;
        Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                String.format("Scrape status monitor starting in state %s", scrapeStatus.toString()));
        while (scrapeStatus != ScrapeStatusEnum.FINISHED && scrapeStatus != ScrapeStatusEnum.ERROR)
        {
            try
            {
                Thread.sleep(1000);
                if (scrapeStatus != prevStatus)
                {
                    prevStatus = scrapeStatus;
                    lastStatusChangeTicks = System.currentTimeMillis();
                    Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                            String.format("Scrape status changed to %s", scrapeStatus.toString()));
                }

                if (System.currentTimeMillis() - lastStatusChangeTicks > SCRAPE_TIMEOUT_THRESHOLD_MILLIS)
                {
                    if (parentLooper != null)
                    {
                        parentLooper.quit();
                    }

                    throw new Exception(String.format("Scraper timed out while in state: %s", scrapeStatus.toString()));
                }
            }
            catch (InterruptedException iex)
            {
                break;
            }
            catch (Exception ex)
            {
                Logger.Log(Logger.LOG_LEVEL_ERROR, LOGTAG,
                        String.format("Caught exception while waiting for scraping to finish: %s", ex.toString()));
                break;
            }
        }
        Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                String.format("Scrape status monitor ending in state %s", scrapeStatus.toString()));

        if (lastScrapeStatus == null)
        {
            switch (scrapeStatus)
            {
            case STARTED:
            case INITIAL_LOAD:
                lastScrapeStatus = "Unable to load Redbox.com to check movies";
                break;

            case LOGIN_LOADED:
            case LOGIN_STARTED:
            case LOGIN_SENT:
                lastScrapeStatus = "Unable to log into Redbox.com using the supplied username/password.";
                break;

            default:
                lastScrapeStatus = "Unable to update movies from Redbox.com";
                break;
            }

        }

        PrefsProxy.setLastScrapeStatus(lastScrapeStatus);
        return null;
    }

    @Override
    public void run()
    {
        // We have to do this scraping in a transient thread. We can't do it directly in the AsyncTask
        // because AsnycTask uses a threadpool. This is a problem because once you have created a
        // Looper on a thread, you cannot create one again later. Furthermore, if you ever stop a Looper
        // on a thread, you can't restart it again later. This means that if we get a threadpool thread
        // that has scraped in the past, then it wouldn't work again. Instead, do the scraping in a
        // transient thread that we re-create every time.
        String LOGTAG = "ScrapeAsync.run";

        Looper.prepare();

        parentLooper = Looper.myLooper();

        // Nuke the WebView's cache and cookies
        try
        {
            ReminderApplication.getContext().deleteDatabase("webview.db");
            ReminderApplication.getContext().deleteDatabase("webviewCache.db");
        }
        catch (Exception e)
        {

        }

        try
        {
            clearCacheFolder(ReminderApplication.getContext().getCacheDir());
        }
        catch (Exception e)
        {

        }

        try
        {
            CookieSyncManager.createInstance(ReminderApplication.getContext());
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
        }
        catch (Exception e)
        {

        }

        final WebView scrapeView = new WebView(ReminderApplication.getContext());

        try
        {
            WebSettings mWebSettings = scrapeView.getSettings();
            mWebSettings.setSavePassword(false);
        }
        catch (Exception e)
        {

        }

        doScrape(scrapeView);

        Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, "Starting scrape loop");
        Looper.loop();
        Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, "Finishing scrape loop");
    }

    // helper method for clearCache() , recursive
    // returns number of deleted files
    private int clearCacheFolder(final File dir)
    {

        int deletedFiles = 0;
        if (dir != null && dir.isDirectory())
        {
            try
            {
                for (File child : dir.listFiles())
                {

                    // first delete subdirectories recursively
                    if (child.isDirectory())
                    {
                        deletedFiles += clearCacheFolder(child);
                    }

                    // then delete the files and subdirectories in this dir
                    // only empty directories can be deleted, so subdirs have been done first
                    if (child.delete())
                    {
                        deletedFiles++;
                    }
                }
            }
            catch (Exception e)
            {
            }
        }
        return deletedFiles;
    }

    @Override
    protected void onPostExecute(Void result)
    {
        if (parentService != null)
        {
            Logger.Log(Logger.LOG_LEVEL_INFO, "ScrapeAsync", "Broadcasting UPDATE_SCRAPE_STATUS Intent");
            parentService.sendBroadcast(new Intent(CheckNotificationsReceiver.ACTION_UPDATE_SCRAPE_STATUS));

            parentService.stopSelf();
        }
        super.onPostExecute(result);
    }

    enum ScrapeStatusEnum
    {
        STARTED, INITIAL_LOAD, LOGIN_LOADED, LOGIN_STARTED, LOGIN_SENT, TXN_LOADED, SCRAPING, FINISHED, ERROR
    }

    private ScrapeStatusEnum    scrapeStatus;

    private static final String REDBOX_TRANSACTIONS_URL = "https://www.redbox.com/account/RentalHistory";

    private static final String LOGIN_EMAIL_ID_REGEX    = "id=\"(login\\d*_Email)\"";
    private static final String LOGIN_PASSWORD_ID_REGEX = "id=\"(login\\d*_Password)\"";
    private static final String LOGIN_ANCHOR_ID_REGEX   = "id=\"(login\\d*_Login)\"";

    private boolean             alreadySeenTxnPage      = false;

    private void doScrape(final WebView scrapeView)
    {

        String LOGTAG = "doScrape";
        scrapeStatus = ScrapeStatusEnum.STARTED;

        final String userEmail = PrefsProxy.getRedboxUserName();
        final String userPassword = PrefsProxy.getRedboxPassword();

        Logger.Log(Logger.LOG_LEVEL_INFO, "ScrapeAsync",
                String.format("Starting doScrape with user/pass: %s/%s", userEmail, userPassword));

        scrapeView.getSettings().setJavaScriptEnabled(true);
        scrapeView
                .getSettings()
                .setUserAgentString(
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.7 (KHTML, like Gecko) Chrome/16.0.912.77 Safari/535.7");

        scrapeView.addJavascriptInterface(new Object()
        {
            @SuppressWarnings("unused")
            public void handleAction(String action, String html)
            {
                String LOGTAG = "doScrape.handleAction";

                Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                        String.format("Received %d bytes of html in state %s", html.length(), action));

                if (action.equals("SCRAPE"))
                {
                    scrapeStatus = ScrapeStatusEnum.SCRAPING;
                    Pattern pattern = Pattern.compile(
                            "transactionhistory\\d+_ItemList(.*)transactionhistory\\d+_Template",
                            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                    Matcher matcher = pattern.matcher(html);

                    if (!matcher.find())
                    {
                        Logger.Log(Logger.LOG_LEVEL_ERROR, "ScrapeHtml", "Unable to find raw txn items");
                        scrapeStatus = ScrapeStatusEnum.ERROR;
                        parentLooper.quit();
                        lastScrapeStatus = "Error while checking Redbox movies: Unable to find rental transaction items.";
                        return;
                    }

                    String txnItemsRaw = matcher.group(1);

                    Logger.Log(Logger.LOG_LEVEL_DEBUG, LOGTAG,
                            String.format("Scraped %d bytes of txn contents: %s", txnItemsRaw.length(), txnItemsRaw));

                    pattern = Pattern.compile("<tr>(.*?)</tr>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                    matcher = pattern.matcher(txnItemsRaw);

                    int newRentals = 0, newReturns = 0;

                    while (matcher.find())
                    {
                        String txnItemsRow = matcher.group(1);

                        Logger.Log(Logger.LOG_LEVEL_DEBUG, LOGTAG,
                                String.format("  - Scraped %d bytes of txn row: %s", txnItemsRow.length(), txnItemsRow));

                        Pattern cellPattern = Pattern.compile("<\\s*td\\s*>\\s*(.*?)\\s*\\s*</td\\s*>",
                                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                        Matcher cellMatcher = cellPattern.matcher(txnItemsRow);

                        ArrayList<String> cells = new ArrayList<String>();

                        while (cellMatcher.find())
                        {
                            String cellContentsRaw = cellMatcher.group(1);

                            Logger.Log(Logger.LOG_LEVEL_DEBUG, LOGTAG,
                                    String.format("    * Scraped %d bytes of txn cell: %s", cellContentsRaw.length(),
                                            cellContentsRaw));

                            String cellContentsClean = getInnerText(cellContentsRaw);
                            Logger.Log(Logger.LOG_LEVEL_DEBUG, LOGTAG,
                                    String.format("      * Clean cell: %s", cellContentsClean));

                            cells.add(cellContentsClean);
                        }

                        if (cells.size() < 5)
                        {
                            Logger.Log(Logger.LOG_LEVEL_ERROR, LOGTAG, String.format(
                                    "Scraped txn contents row, but found only %d cells (expected at least 5)",
                                    cells.size()));
                            continue;
                        }

                        String movieName = cells.get(0);
                        Date rentalDate = MovieListProxy.convertRedboxDate(cells.get(2));
                        Date returnDate = MovieListProxy.convertRedboxDate(cells.get(3));
                        String returnStatus = cells.get(4);

                        Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format(
                                "  - Movie Scrape Results: Name: %s, RentedOn: %s, ReturnedOn: %s", movieName,
                                rentalDate, returnDate));

                        int result = MovieListProxy.RedboxScrapeHandleResults(movieName, rentalDate, returnDate,
                                returnStatus);

                        if (result == MovieListProxy.RENTAL_STATUS_NEW_RENTAL)
                        {
                            newRentals++;
                        }

                        if (result == MovieListProxy.RENTAL_STATUS_NEW_RETURN)
                        {
                            newReturns++;
                        }
                    }

                    if (newReturns + newRentals > 0)
                    {
                        lastScrapeStatus = String.format(
                                "Redbox movies checked successfully.  %d new rentals added, %d rentals returned.",
                                newRentals, newReturns);
                    }
                    else
                    {
                        lastScrapeStatus = "Redbox movies checked successfully.  No changes were found.";
                    }

                    // The more verbose text above takes too much room on smaller devices, and doesn't
                    // really add anything for most users.
                    lastScrapeStatus = "Redbox movies checked successfully.";

                    Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, "Scraping is finished successfully");
                    scrapeStatus = ScrapeStatusEnum.FINISHED;
                    parentLooper.quit();
                    return;
                }

                if (action.equals("BAD_LOGIN"))
                {
                    if (html.contains("Sign in failed"))
                    {
                        Logger.Log(Logger.LOG_LEVEL_ERROR, "ScrapeHtml", "Saw 'sign in failed' on login page");
                        lastScrapeStatus = "Redbox.com sign in failed.";
                        scrapeStatus = ScrapeStatusEnum.ERROR;
                        parentLooper.quit();
                        return;
                    }
                }

                if (action.equals("LOGIN"))
                {
                    scrapeStatus = ScrapeStatusEnum.LOGIN_STARTED;
                    Pattern loginEmailPattern = Pattern.compile(LOGIN_EMAIL_ID_REGEX);
                    Matcher loginEmailMatcher = loginEmailPattern.matcher(html);

                    if (!loginEmailMatcher.find())
                    {
                        Logger.Log(Logger.LOG_LEVEL_ERROR, "ScrapeHtml", "Unable to find login email id");
                        scrapeStatus = ScrapeStatusEnum.ERROR;
                        parentLooper.quit();
                        lastScrapeStatus = "Error while checking Redbox movies: Unable to find login username field.";
                        return;
                    }

                    String emailId = loginEmailMatcher.group(1);

                    Pattern loginPasswordPattern = Pattern.compile(LOGIN_PASSWORD_ID_REGEX);
                    Matcher loginPasswordMatcher = loginPasswordPattern.matcher(html);

                    if (!loginPasswordMatcher.find())
                    {
                        Logger.Log(Logger.LOG_LEVEL_ERROR, "ScrapeHtml", "Unable to find login password id");
                        scrapeStatus = ScrapeStatusEnum.ERROR;
                        parentLooper.quit();
                        lastScrapeStatus = "Error while checking Redbox movies: Unable to find login password field.";
                        return;
                    }

                    String passwordId = loginPasswordMatcher.group(1);

                    Pattern loginAnchorPattern = Pattern.compile(LOGIN_ANCHOR_ID_REGEX);
                    Matcher loginAnchorMatcher = loginAnchorPattern.matcher(html);

                    if (!loginAnchorMatcher.find())
                    {
                        Logger.Log(Logger.LOG_LEVEL_ERROR, "ScrapeHtml", "Unable to find login anchor id");
                        scrapeStatus = ScrapeStatusEnum.ERROR;
                        parentLooper.quit();
                        lastScrapeStatus = "Error while checking Redbox movies: Unable to find login button.";
                        return;
                    }

                    String anchorId = loginAnchorMatcher.group(1);

                    Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String
                            .format("Calling JavaScript login with user: %s, Pass: %s, Link: %s", emailId, passwordId,
                                    anchorId));

                    scrapeView.loadUrl(String.format("javascript:(function(){ "
                            + "    document.getElementById('%s').value = '%s'; "
                            + "    document.getElementById('%s').value = '%s'; "
                            + "    var myEvt = document.createEvent('MouseEvents'); "
                            + "    myEvt.initEvent('click',true,true); "
                            + "    document.getElementById('%s').dispatchEvent(myEvt); " + "})();", emailId, userEmail,
                            passwordId, userPassword, anchorId));
                    scrapeStatus = ScrapeStatusEnum.LOGIN_SENT;

                    return;
                }

                Logger.Log(Logger.LOG_LEVEL_ERROR, LOGTAG, String.format("Unknown injectHTML action: %s", action));
                scrapeStatus = ScrapeStatusEnum.ERROR;
                lastScrapeStatus = "Error while checking Redbox movies: Unknown login error.";
                parentLooper.quit();
            }

        }, "MY_RB_JS");

        scrapeView.setWebChromeClient(new WebChromeClient()
        {

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage)
            {
                String LOGTAG = "doScrape.onConsoleMessage";

                Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format("ConsoleMessage: %s", consoleMessage.message()));
                return super.onConsoleMessage(consoleMessage);
            }

        });

        scrapeView.setWebViewClient(new WebViewClient()
        {

            @Override
            public void onPageFinished(WebView view, String url)
            {
                String LOGTAG = "doScrape.setWebView";

                super.onPageFinished(view, url);

                url = url.toLowerCase();

                Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format("Got page finished event for url: %s", url));

                if (url.contains("register?returnurl"))
                {
                    if (scrapeStatus == ScrapeStatusEnum.LOGIN_LOADED)
                    {
                        // We've come back to the login page a second time. Check if it was a failed login
                        Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format(
                                "URL %s finished loading for the second time, injecting BAD_LOGIN call", url));
                        view.loadUrl("javascript:setTimeout('window.MY_RB_JS.handleAction(\"BAD_LOGIN\", document.body.innerHTML);', 50);");

                    }

                    scrapeStatus = ScrapeStatusEnum.LOGIN_LOADED;

                    Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                            String.format("URL %s finished loading, injecting JS 5 second wait LOGIN call", url));
                    view.loadUrl("javascript:setTimeout('window.MY_RB_JS.handleAction(\"LOGIN\", document.body.innerHTML);', 5000);");
                    return;
                }

                if (url.contains("account/rentalhistory"))
                {
                    if (alreadySeenTxnPage) { return; }

                    scrapeStatus = ScrapeStatusEnum.TXN_LOADED;
                    alreadySeenTxnPage = true;
                    Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                            String.format("URL %s finished loading, injecting JS 5 second wait SCRAPE call", url));
                    view.loadUrl("javascript:setTimeout('window.MY_RB_JS.handleAction(\"SCRAPE\", document.body.innerHTML);', 5000);");
                    return;
                }

                Logger.Log(Logger.LOG_LEVEL_ERROR, LOGTAG,
                        String.format("Error, unknown URL in onPageFinished: %s", url));
                scrapeStatus = ScrapeStatusEnum.ERROR;
                parentLooper.quit();
                lastScrapeStatus = "Error while checking Redbox movies: Unknown page loaded.";
            }
        });

        scrapeStatus = ScrapeStatusEnum.INITIAL_LOAD;

        Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format("Loading URL %s", REDBOX_TRANSACTIONS_URL));
        scrapeView.loadUrl(REDBOX_TRANSACTIONS_URL);
    }

    protected String getInnerText(String htmlFragment)
    {
        if (htmlFragment == null || htmlFragment.trim().length() < 1) { return ""; }

        StringBuilder retval = new StringBuilder();
        int angleDepth = 0;

        for (int index = 0; index < htmlFragment.length(); index++)
        {
            char htmlChar = htmlFragment.charAt(index);

            if (htmlChar == '<')
            {
                angleDepth++;
                continue;
            }
            if (htmlChar == '>')
            {
                angleDepth--;
                if (angleDepth < 0)
                {
                    angleDepth = 0;
                }
                continue;
            }
            if (angleDepth == 0)
            {
                retval.append(htmlChar);
            }
        }

        return retval.toString();
    }
}
