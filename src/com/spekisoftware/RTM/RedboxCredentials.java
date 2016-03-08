package com.spekisoftware.RTM;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;

public class RedboxCredentials extends CustomTitleActivity
{
    private AlertDialog infoDialog;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        final RedboxCredentials parent = this;

        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.redbox_credentials);

        Button okButton = (Button)findViewById(R.id.OkButton);
        okButton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                parent.OkClick();
            }

        });

        Button cancelButton = (Button)findViewById(R.id.CancelButton);
        cancelButton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                finish();
            }

        });

        String infoText = getString(R.string.ScrapeInfoText);
        WebView wv = new WebView(getBaseContext());
        wv.loadData(infoText, "text/html", null);
        wv.setBackgroundColor(Color.TRANSPARENT);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // builder.setMessage(R.string.ScrapeInfoText);
        builder.setView(wv);
        builder.setIcon(R.drawable.ic_menu_help);
        builder.setTitle(R.string.ScrapeInfoTitle);
        builder.setNeutralButton(R.string.ScrapeInfoOkButton, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });

        infoDialog = builder.create();
        infoDialog.setOwnerActivity(this);

        Button scrapeInfoButton = (Button)findViewById(R.id.AutoScrapeInfoButton);

        scrapeInfoButton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                infoDialog.show();
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        EditText redboxUsernameText = (EditText)findViewById(R.id.RedboxUsernameText);
        EditText redboxPasswordText = (EditText)findViewById(R.id.RedboxPasswordText);

        if (PrefsProxy.hasRedboxCredentials())
        {
            redboxUsernameText.setText(PrefsProxy.getRedboxUserName());
            redboxPasswordText.setText(PrefsProxy.getRedboxPassword());
        }
        else
        {
            redboxUsernameText.setText("");
            redboxPasswordText.setText("");
        }
    }

    private static final int DIALOG_INVALID_USERNAME = 1;
    private static final int DIALOG_NEED_BOTH_CREDS = 2;

    @Override
    protected Dialog onCreateDialog(int id)
    {
        super.onCreateDialog(id);

        if (id == DIALOG_INVALID_USERNAME)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.InvalidRedboxUsername).setCancelable(false)
                    .setNeutralButton(R.string.Ok, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            dialog.dismiss();
                        }
                    });
            return builder.create();
        }

        if (id == DIALOG_NEED_BOTH_CREDS)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.NeedBothCreds).setCancelable(false)
                    .setNeutralButton(R.string.Ok, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            dialog.dismiss();
                        }
                    });
            return builder.create();
        }

        return null;
    }

    private void OkClick()
    {
        EditText redboxUsernameText = (EditText)findViewById(R.id.RedboxUsernameText);
        EditText redboxPasswordText = (EditText)findViewById(R.id.RedboxPasswordText);

        String redboxUsername = redboxUsernameText.getText().toString();
        String redboxPassword = redboxPasswordText.getText().toString();
        
        if((redboxUsername == null || redboxUsername.length() < 1) 
                && (redboxPassword == null || redboxPassword.length() < 1))
        {
            PrefsProxy.setRedboxCredentials(null,  null);
            PrefsProxy.setLastScrapeStatus(null);
            ChangePrefs.SetScrapeAlarm(true);
            finish();
            return;
        }
        
        if((redboxUsername == null || redboxUsername.length() < 1) 
                || (redboxPassword == null || redboxPassword.length() < 1))
        {
            showDialog(DIALOG_NEED_BOTH_CREDS);
            return;
        }
        
        Pattern pattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(redboxUsername);

        if (!matcher.find())
        {
            showDialog(DIALOG_INVALID_USERNAME);
            return;
        }

        PrefsProxy.setRedboxCredentials(redboxUsername, redboxPassword);

        PrefsProxy.setLastScrapeStatus(null);

        ChangePrefs.SetScrapeAlarm(true);

        finish();
    }
}
