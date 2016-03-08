package com.spekisoftware.RTM;

import java.util.ArrayList;
import java.util.Date;

import com.spekisoftware.RTM.RESTHandler.RESTResponse;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class NewRental extends CustomTitleActivity
{
    private static final int       AUTOCOMPLETE_DELAY_MILLIS = 1000;

    private AutoCompleteTextView   movieNameText;
    private MovieACAdapter<String> movieACAdapter;
    private long                   acTimestamp;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_rental_view);

        final Context parent = this;

        if (ScreenWidthPx < 480)
        {
            // On a 320 width device, he autoupdate guy is just a bit too tall, he overlaps the
            // Remind Me button. Scoot him down a snoodge
            ImageView figureView = (ImageView)findViewById(R.id.autoUpdateGuy);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)(figureView.getLayoutParams());
            params.bottomMargin = -15;
        }

        Button redboxLoginButton = (Button)findViewById(R.id.redboxLoginButton);
        redboxLoginButton.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                Intent redboxIntent = new Intent(parent, RedboxCredentials.class);
                startActivity(redboxIntent);
            }

        });

        Button rentButton = (Button)findViewById(R.id.rentItButton);
        rentButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AutoCompleteTextView movieNameText = (AutoCompleteTextView)findViewById(R.id.movieNameText);
                String movieName = movieNameText.getText().toString();

                if (movieName.equals("DEBUG_ADD"))
                {
                    MovieListProxy.AddTestMovieData();
                }
                else if (movieName.equals("DEBUG_CLEAR"))
                {
                    MovieListProxy.ClearMovieData();
                }
                else if (!movieName.equals(getString(R.string.LoadingMovieSuggestions))
                        && !movieName.equals(getString(R.string.NoMovieSuggestions))
                        && !movieName.equals(getString(R.string.ErrorGettingMovieSuggestions)))
                {
                    MovieListProxy.RentMovie(movieName, new Date(), MovieListProxy.SOURCE_MANUAL);
                }

                finish();
            }
        });

        Button neverMindButton = (Button)findViewById(R.id.neverMindButton);
        neverMindButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });

        movieACAdapter = new MovieACAdapter<String>(this, android.R.layout.simple_dropdown_item_1line);

        movieNameText = (AutoCompleteTextView)findViewById(R.id.movieNameText);
        movieNameText.setThreshold(3);
        movieNameText.setAdapter(movieACAdapter);

        final AutocompleteRunner acRunner = new AutocompleteRunner(movieNameText);

        movieNameText.addTextChangedListener(new TextWatcher()
        {
            String LOGTAG = "AutoCompleteTextWatcher";

            @Override
            public void afterTextChanged(Editable s)
            {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, "onTextChanged: Checking for autocomplete");

                if (movieNameText.isPerformingCompletion())
                {
                    Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, "isPerformingCompletion is true, skipping autocomplete");
                    return;
                }

                if (s.length() < movieNameText.getThreshold())
                {
                    Logger.Log(
                            Logger.LOG_LEVEL_INFO,
                            LOGTAG,
                            String.format("text length %d is less than threshold %d, skipping autocompleter",
                                    s.length(), movieNameText.getThreshold()));
                    return;
                }

                if (!containsAlpha(s))
                {
                    Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                            String.format("Skipping autocompleter because the query '%s' contains no letters", s));
                    return;
                }

                acTimestamp = System.currentTimeMillis();
                movieNameText.removeCallbacks(acRunner);
                movieNameText.postDelayed(acRunner, AUTOCOMPLETE_DELAY_MILLIS);
                movieACAdapter.clear();
                movieACAdapter.add(getString(R.string.LoadingMovieSuggestions));
                movieACAdapter.notifyDataSetChanged();
                Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                        String.format("Text changed, removing and re-posting acRunner with timestamp: %d", acTimestamp));
            }

            private boolean containsAlpha(CharSequence s)
            {
                for (int checkIndex = 0; checkIndex < s.length(); checkIndex++)
                {
                    if (Character.isLetter(s.charAt(checkIndex))) { return true; }
                }
                return false;
            }

        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        ImageView autoUpdateGuy = (ImageView)findViewById(R.id.autoUpdateGuy);
        Button redboxLoginButton = (Button)findViewById(R.id.redboxLoginButton);

        if (!PrefsProxy.hasRedboxCredentials())
        {
            Drawable signupGuy = getResources().getDrawable(R.drawable.redbox_signup_guy);
            int signupGuyWidth = signupGuy.getIntrinsicWidth();
            int signupGuyHeight = signupGuy.getIntrinsicHeight();

            double signupGuyRatio = ScreenWidthPx / signupGuyWidth;
            signupGuyHeight *= signupGuyRatio;

            Logger.Log(Logger.LOG_LEVEL_DEBUG, "NewRental",
                    String.format("Setting signup guy dimensions to: %d, %d", (int)ScreenWidthPx, signupGuyHeight));
            autoUpdateGuy.getLayoutParams().width = (int)ScreenWidthPx;
            autoUpdateGuy.getLayoutParams().height = signupGuyHeight;
            autoUpdateGuy.setVisibility(View.VISIBLE);
            redboxLoginButton.setVisibility(View.VISIBLE);
        }
        else
        {
            autoUpdateGuy.setVisibility(View.GONE);
            redboxLoginButton.setVisibility(View.GONE);
        }
    }

    private class AutocompleteRunner implements Runnable
    {
        AutoCompleteTextView acView;

        public AutocompleteRunner(AutoCompleteTextView view)
        {
            acView = view;
        }

        @Override
        public void run()
        {
            final long saveACTimestamp = acTimestamp;

            final String LOGTAG = "AutoCompleteRunner";

            Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, "ACRunner started");

            String queryString = acView.getText().toString();

            if (queryString.length() < acView.getThreshold())
            {
                Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                        "ACRunner declining to run autocomplete due to query length threshold");
                return;
            }

            Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                    String.format("ACRunner updating movie list from query: %s", queryString));

            TMDBAPI api = new TMDBAPI();

            api.AutocompleteMovies(queryString, new TMDBAPI.AutocompleteCallback()
            {
                @Override
                public void callback(ArrayList<String> result)
                {
                    if (acTimestamp != saveACTimestamp)
                    {
                        Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                                "ACRunner: Throwing away results because of acTimestamp mismatch");
                        return;
                    }

                    Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                            String.format("Got autocomplete result with %d items", result.size()));
                    movieACAdapter.clear();

                    if (result.size() == 0)
                    {
                        movieACAdapter.add(getString(R.string.NoMovieSuggestions));
                    }
                    else
                    {
                        for (String s : result)
                            movieACAdapter.add(s);
                    }

                    Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, "Notifiying movieACAdapter of change");
                    movieACAdapter.notifyDataSetChanged();
                }
            }, new RESTHandler.RESTCallback()
            {
                @Override
                public void callback(RESTResponse result)
                {
                    if (result.isError)
                    {
                        Logger.Log(Logger.LOG_LEVEL_ERROR, LOGTAG,
                                String.format("RESTHandler returned error: %s", result.exceptionMessage));

                        if (acTimestamp != saveACTimestamp)
                        {
                            Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                                    "ACRunner: Throwing away results because of acTimestamp mismatch");
                            return;
                        }

                        movieACAdapter.clear();
                        movieACAdapter.add(getString(R.string.ErrorGettingMovieSuggestions));

                        Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, "Notifiying movieACAdapter of change");
                        movieACAdapter.notifyDataSetChanged();

                        return;
                    }
                }
            });
        }
    }
}
