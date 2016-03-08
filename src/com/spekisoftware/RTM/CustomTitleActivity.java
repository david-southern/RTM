package com.spekisoftware.RTM;

import java.util.Hashtable;

import com.zubhium.ZubhiumSDK;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.MailTo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class CustomTitleActivity extends Activity
{
    ZubhiumSDK       sdk;

    protected double ScreenWidthPx;
    protected double BannerRatio;

    private View     customTitleView = null;
    private int      FigureHeightPx;

    // private static int currentImageIndex = 0;

    private int[]    titleImages     = { R.drawable.figure_01, R.drawable.figure_02, R.drawable.figure_03,
            R.drawable.figure_04, R.drawable.figure_05, R.drawable.figure_06, R.drawable.figure_07,
            R.drawable.figure_08, R.drawable.figure_10, R.drawable.figure_11, R.drawable.figure_12,
            R.drawable.figure_13, R.drawable.figure_14, R.drawable.figure_16, R.drawable.figure_17,
            R.drawable.figure_18, R.drawable.figure_19, R.drawable.figure_20, R.drawable.figure_21,
            R.drawable.figure_22, R.drawable.figure_23, R.drawable.figure_24, R.drawable.figure_26,
            R.drawable.figure_27, R.drawable.figure_28, R.drawable.figure_29, R.drawable.figure_30,
            R.drawable.figure_31, R.drawable.figure_32, R.drawable.figure_33, R.drawable.figure_34,
            R.drawable.figure_35, R.drawable.figure_36, R.drawable.figure_37, R.drawable.figure_38,
            R.drawable.figure_39    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        sdk = ZubhiumSDK.getZubhiumSDKInstance(this, "4580d6df45a28be4be5a7da8446e6b");

    }

    @Override
    protected void onResume()
    {
        super.onResume();

        String LOGTAG = "CustomTitle";

        if (customTitleView == null)
        {
            LayoutInflater inflater;
            inflater = LayoutInflater.from(this);
            LinearLayout titlePlaceholder = (LinearLayout)findViewById(R.id.TitlePlaceholder);
            customTitleView = inflater.inflate(R.layout.custom_title, titlePlaceholder);

            WindowManager wm = ((WindowManager)this.getSystemService(Context.WINDOW_SERVICE));
            Display mDisplay = wm.getDefaultDisplay();
            ScreenWidthPx = mDisplay.getWidth();
            // ScreenHeightPx = mDisplay.getHeight();

            Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format("Detected screen width: %d", (int)ScreenWidthPx));

            int bannerImageId = R.drawable.custom_title_image_480;
            double figureHeightRatio = 0.78;
            double[] figureRightOffsets = { -10, -10, 0.16 };

            if (ScreenWidthPx >= 800)
            {
                bannerImageId = R.drawable.custom_title_image_800;
                figureHeightRatio = 0.78;
                figureRightOffsets[0] = 0.15;
                figureRightOffsets[1] = 0.261;
                figureRightOffsets[2] = 0.375;
            }
            else if (ScreenWidthPx >= 600)
            {
                bannerImageId = R.drawable.custom_title_image_600;
                figureHeightRatio = 0.78;
                figureRightOffsets[0] = -10;
                figureRightOffsets[1] = 0.1425;
                figureRightOffsets[2] = 0.297;
            }
            else if (ScreenWidthPx < 480)
            {
                figureHeightRatio = 0.79;
            }

            Drawable bannerImage = getResources().getDrawable(bannerImageId);
            int bannerWidth = bannerImage.getIntrinsicWidth();
            int bannerHeight = bannerImage.getIntrinsicHeight();

            Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                    String.format("Detected banner size: %d, %d", bannerWidth, bannerHeight));

            BannerRatio = ScreenWidthPx / bannerWidth;
            bannerHeight *= BannerRatio;

            FigureHeightPx = (int)(bannerHeight * figureHeightRatio);
            int figureTopPadding = (int)(bannerHeight * 0.05);
            ImageView bannerView = (ImageView)findViewById(R.id.BannerImage);
            bannerView.setImageDrawable(bannerImage);

            Logger.Log(Logger.LOG_LEVEL_INFO, "CustomTitle",
                    String.format("Setting custom title height to %d", bannerHeight));
            customTitleView.getLayoutParams().height = bannerHeight;

            ImageView figureView = (ImageView)findViewById(R.id.CustomTitleFig2);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)(figureView.getLayoutParams());
            params.height = FigureHeightPx;
            params.width = FigureHeightPx;
            params.rightMargin = (int)(ScreenWidthPx - figureRightOffsets[2] * ScreenWidthPx);
            figureView.setPadding(0, figureTopPadding, 0, 0);
            Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                    String.format("Moving figure 2 to %d X, height %d Y", params.rightMargin, FigureHeightPx));

            figureView = (ImageView)findViewById(R.id.CustomTitleFig1);

            if (figureRightOffsets[1] < 0)
            {
                figureView.setVisibility(View.GONE);
            }
            else
            {
                params = (RelativeLayout.LayoutParams)(figureView.getLayoutParams());
                params.height = FigureHeightPx;
                params.width = FigureHeightPx;
                params.rightMargin = (int)(ScreenWidthPx - figureRightOffsets[1] * ScreenWidthPx);
                figureView.setPadding(0, figureTopPadding, 0, 0);
                Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format("Moving figure 1 to %d X", params.rightMargin));
            }

            figureView = (ImageView)findViewById(R.id.CustomTitleFig0);

            if (figureRightOffsets[0] < 0)
            {
                figureView.setVisibility(View.GONE);
            }
            else
            {
                params = (RelativeLayout.LayoutParams)(figureView.getLayoutParams());
                params.rightMargin = (int)(ScreenWidthPx - figureRightOffsets[0] * ScreenWidthPx);
                params.height = FigureHeightPx;
                params.width = FigureHeightPx;
                figureView.setPadding(0, figureTopPadding, 0, 0);
                figureView.setAlpha(64);
                Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format("Moving figure 0 to %d X", params.rightMargin));
            }
        }

        int imageId = titleImages[(int)(Math.random() * titleImages.length)];

        // currentImageIndex++;
        //
        // if (currentImageIndex >= titleImages.length)
        // {
        // currentImageIndex = 0;
        // }
        //
        // imageId = titleImages[currentImageIndex];

        if (BannerRatio > 1.0001 || BannerRatio < 0.9998)
        {
            BitmapDrawable figureImage = (BitmapDrawable)getResources().getDrawable(imageId);

            Logger.Log(
                    Logger.LOG_LEVEL_DEBUG,
                    "CustomTitle",
                    String.format("FigHeight: %d, imageDim: (%d, %d), scaleFactor: %.3f", FigureHeightPx,
                            figureImage.getIntrinsicWidth(), figureImage.getIntrinsicHeight(), BannerRatio));

            int newHeight = (int)Math.round(figureImage.getIntrinsicHeight() * BannerRatio);
            int newWidth = (int)Math.round(figureImage.getIntrinsicWidth() * BannerRatio);

            Logger.Log(Logger.LOG_LEVEL_DEBUG, "CustomTitle",
                    String.format("New BitmapDim: (%d, %d)", newWidth, newHeight));

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(figureImage.getBitmap(), newWidth, newHeight, true);

            Logger.Log(
                    Logger.LOG_LEVEL_DEBUG,
                    "CustomTitle",
                    String.format("FigureDim: (%d, %d)", figureImage.getIntrinsicWidth(),
                            figureImage.getIntrinsicHeight()));

            // Make sure that these ImageViews do not get set from the same Drawable.  For some reason,
            // attributes set on the parent ImageView seem to be shared down to the Drawable, so if they
            // all use the same drawable then they all get the same alpha value, which is a problem for Fig0
            ImageView iv = ((ImageView)findViewById(R.id.CustomTitleFig0));
            iv.setImageDrawable(new BitmapDrawable(scaledBitmap));
            iv = ((ImageView)findViewById(R.id.CustomTitleFig1));
            iv.setImageDrawable(new BitmapDrawable(scaledBitmap));
            iv = ((ImageView)findViewById(R.id.CustomTitleFig2));
            iv.setImageDrawable(new BitmapDrawable(scaledBitmap));
        }
        else
        {
            ImageView iv = ((ImageView)findViewById(R.id.CustomTitleFig0));
            iv.setImageResource(imageId);
            iv = ((ImageView)findViewById(R.id.CustomTitleFig1));
            iv.setImageResource(imageId);
            iv = ((ImageView)findViewById(R.id.CustomTitleFig2));
            iv.setImageResource(imageId);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        // Ignore orientation changes
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.application_menu, menu);
        return true;
    }

    private static final String VERSION_TOKEN = "{{version}}";

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case R.id.menuItemAbout:
            String infoText = getString(R.string.AboutScreenHTML);

            String versionName = "";

            try
            {
                String pkg = this.getPackageName();
                versionName = "v" + this.getPackageManager().getPackageInfo(pkg, 0).versionName;
            }
            catch (Exception e)
            {
            }

            Hashtable<String, String> tokens = new Hashtable<String, String>();

            tokens.put(VERSION_TOKEN, versionName);

            infoText = Utils.ReplaceTokens(infoText, tokens);

            WebView wv = new WebView(this);
            wv.loadData(infoText, "text/html", null);
            wv.setBackgroundColor(Color.TRANSPARENT);

            WebViewClient mWebClient = new WebViewClient()
            {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url)
                {
                    if (url.startsWith("mailto:"))
                    {
                        MailTo mt = MailTo.parse(url);
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.putExtra(Intent.EXTRA_EMAIL, new String[] { mt.getTo() });
                        intent.setType("message/rfc822");
                        startActivity(Intent.createChooser(intent, "Send mail..."));
                        view.reload();
                        return true;
                    }
                    else
                    {
                        Intent hrefIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(hrefIntent);
                        return true;
                    }
                }
            };
            wv.setWebViewClient(mWebClient);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(wv);
            builder.setIcon(R.drawable.ic_menu_info_details);
            builder.setTitle(R.string.AboutScreenTitle);
            builder.setNeutralButton(R.string.ScrapeInfoOkButton, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.cancel();
                }
            });

            AlertDialog infoDialog = builder.create();
            infoDialog.setOwnerActivity(this);

            infoDialog.show();

            return true;
        case R.id.menuItemSettings:
            Intent prefsIntent = new Intent(this, ChangePrefs.class);
            startActivity(prefsIntent);
            return true;
        case R.id.menuItemNewRental:
            Intent rentIntent = new Intent(this, NewRental.class);
            startActivity(rentIntent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSearchRequested()
    {
        Intent rentIntent = new Intent(this, NewRental.class);
        startActivity(rentIntent);
        return false;
    }

}
