package com.spekisoftware.RTM;

import java.util.ArrayList;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MovieAdapter extends BaseAdapter
{
    private LayoutInflater   inflater;
    private ArrayList<Movie> movieList;
    private Context          context;
    private MovieAdapter     myself;

    private DataSetObserver  observer = new DataSetObserver()
                                      {
                                          @Override
                                          public void onChanged()
                                          {
                                              Logger.Log(Logger.LOG_LEVEL_INFO, "MovieAdapter",
                                                      "Invalidating movie ListView in response to MovieListProxy change notification");
                                              myself.updateMovieList();
                                          }
                                      };

    public MovieAdapter(Context context)
    {
        myself = this;

        inflater = LayoutInflater.from(context);

        updateMovieList();
        this.context = context;
        this.reminderView = false;

        MovieListProxy.RegisterChangeObserver(observer);
    }

    public void UnRegisterMovieObserver()
    {
        MovieListProxy.UnRegisterChangeObserver(observer);
    }

    public void updateMovieList()
    {
        ArrayList<Movie> movies = MovieListProxy.LoadMovieList();
        this.movieList = movies;
        
        long earliestUndoTimestamp = Long.MAX_VALUE;
        
        for(Movie checkUndoMovie : movies)
        {
            if(checkUndoMovie.getUndoRentalTimestamp() > 0)
            {
                if(checkUndoMovie.getUndoRentalTimestamp() < earliestUndoTimestamp)
                {
                    earliestUndoTimestamp = checkUndoMovie.getUndoRentalTimestamp();
                }
            }
        }
        
        if(earliestUndoTimestamp != Long.MAX_VALUE)
        {
            // Make sure that the broadcast happens slightly after the undo so that it will remove the movie
            // correctly
            earliestUndoTimestamp += 50;
            
            Logger.Log(Logger.LOG_LEVEL_DEBUG, "MovieAdapter", String.format("Scheduling undo list update for +%.2f seconds", 
                    (earliestUndoTimestamp - System.currentTimeMillis()) / 1000.0f ));
            Intent intent = new Intent(CheckNotificationsReceiver.ACTION_UPDATE_SCRAPE_STATUS);
            PendingIntent pendIntent = PendingIntent.getBroadcast(ReminderApplication.getContext(), 0, intent, 0);

            AlarmManager mgr = (AlarmManager)ReminderApplication.getContext().getSystemService(Context.ALARM_SERVICE);

            mgr.set(AlarmManager.RTC, earliestUndoTimestamp, pendIntent);
        
        }
        
        notifyDataSetChanged();
    }

    private boolean reminderView;

    public boolean isReminderView()
    {
        return reminderView;
    }

    public void setReminderView(boolean reminderView)
    {
        this.reminderView = reminderView;
    }

    @Override
    public int getCount()
    {
        return movieList.size();
    }

    @Override
    public Object getItem(int position)
    {
        return movieList.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final MovieAdapter parentAdapter = this;

        ViewHolder holder;

        if (convertView == null)
        {
            convertView = inflater.inflate(R.layout.movie_list_item, null);

            holder = new ViewHolder();
            holder.movieNameText = (TextView)convertView.findViewById(R.id.movieTitleText);
            holder.rentalDateText = (TextView)convertView.findViewById(R.id.rentalDateText);
            holder.rentalAgeText = (TextView)convertView.findViewById(R.id.rentalAgeText);
            holder.returnButton = (Button)convertView.findViewById(R.id.MovieReturnedButton);
            holder.movieImage = (ImageView)convertView.findViewById(R.id.movieImage);
            holder.returnedImage = (ImageView)convertView.findViewById(R.id.ReturnedStamp);
            holder.initialTextSize = holder.movieNameText.getTextSize();
            holder.returnButton.setTag(holder);

            final ViewHolder returnHolder = holder;

            holder.returnButton.setOnClickListener(new OnClickListener()
            {

                @Override
                public void onClick(View v)
                {
                    Logger.Log(
                            Logger.LOG_LEVEL_INFO,
                            "MovieAdapter",
                            String.format("Got click event on return button, returning movie: %s",
                                    returnHolder.movie.getName()));
                    MovieListProxy.ReturnMovie(returnHolder.movie);
                    parentAdapter.updateMovieList();
                }

            });

            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder)convertView.getTag();
        }

        Movie viewMovie = movieList.get(position);
        holder.movieNameText.setText(viewMovie.getName());
        
        Logger.Log(Logger.LOG_LEVEL_DEBUG, "MovieAdapter", String.format("Movie length: %d - %s", viewMovie.getName().length(), viewMovie.getName()));
        if(viewMovie.getName().length() > 30)
        {
            float smallSize = holder.initialTextSize * 0.66f;
            
            Logger.Log(Logger.LOG_LEVEL_DEBUG, "MovieAdapter", String.format("Settign text size to: %.2f", smallSize));
            holder.movieNameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallSize);
        }
        else
            if(viewMovie.getName().length() > 18)
            {
                float smallSize = holder.initialTextSize * 0.75f;
                
                Logger.Log(Logger.LOG_LEVEL_DEBUG, "MovieAdapter", String.format("Settign text size to: %.2f", smallSize));
                holder.movieNameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallSize);
            }
            else
        {
            Logger.Log(Logger.LOG_LEVEL_DEBUG, "MovieAdapter", String.format("Settign text size to: %.2f", holder.initialTextSize));
            holder.movieNameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.initialTextSize);
        }
        
        if (holder.rentalAgeText == null)
        {
            holder.rentalDateText.setText(getRentalDateAndAgeString(viewMovie));
        }
        else
        {
            holder.rentalDateText.setText(getRentalDateString(viewMovie));
            holder.rentalAgeText.setText(getRentalAgeString(viewMovie));
        }

        int backgroundResource = 0;
        int textColor = Color.BLACK;

        int movieAge = viewMovie.getAge();
        int targetMovieAge = PrefsProxy.getNotifyPeriod();

        if (movieAge < targetMovieAge)
        {
            backgroundResource = R.drawable.gray_list_background;
        }
        else
        {
            backgroundResource = R.drawable.red_list_background;
        }

        if (viewMovie.getUndoRentalTimestamp() > 0)
        {
            backgroundResource = R.drawable.gray_list_background;
            textColor = Color.LTGRAY;
            holder.returnButton.setText(R.string.Undo);
            Logger.Log(
                    Logger.LOG_LEVEL_INFO,
                    "MovieAdapter",
                    String.format("Setting movie %s UNDO button, undo timestamp: %d", viewMovie.getName(),
                            viewMovie.getUndoRentalTimestamp() - System.currentTimeMillis()));
            holder.returnedImage.setVisibility(View.VISIBLE);
        }
        else
        {
            Logger.Log(Logger.LOG_LEVEL_INFO, "MovieAdapter",
                    String.format("Setting movie %s RETURNED button", viewMovie.getName()));
            holder.returnButton.setText(R.string.Returned);
            holder.returnedImage.setVisibility(View.GONE);
        }

        convertView.setBackgroundResource(backgroundResource);
        holder.movieNameText.setTextColor(textColor);
        holder.rentalDateText.setTextColor(textColor);

        if (holder.rentalAgeText != null)
        {
            holder.rentalAgeText.setTextColor(textColor);
        }

        Bitmap movieImage = viewMovie.getImage();

        if (movieImage == null)
        {
            holder.movieImage.setImageResource(R.drawable.movie_icon);
        }
        else
        {
            holder.movieImage.setImageBitmap(movieImage);
        }

        holder.movie = viewMovie;

        return convertView;
    }

    public String getRentalDateAndAgeString(Movie movie)
    {
        String retval = "Rented: "
                + DateUtils
                        .formatDateTime(this.context, movie.getRentalDate().getTime(), DateUtils.FORMAT_ABBREV_MONTH)
                + " - ";

        int movieAge = movie.getAge();

        if (movieAge == 0)
        {
            retval += context.getString(R.string.Today);
        }
        else if (movieAge == 1)
        {
            retval += context.getString(R.string.Yesterday);
        }
        else
        {
            retval += movieAge + " ";
            retval += context.getString(R.string.DaysAgo);
        }

        return retval;
    }

    public String getRentalDateString(Movie movie)
    {
        String retval = "Rented: "
                + DateUtils
                        .formatDateTime(this.context, movie.getRentalDate().getTime(), DateUtils.FORMAT_ABBREV_MONTH);
        return retval;
    }

    public String getRentalAgeString(Movie movie)
    {
        String retval = "Duration: ";

        int movieAge = movie.getAge();

        retval += movieAge;

        if (movieAge == 1)
        {
            retval += " " + context.getString(R.string.DayAgo);
        }
        else
        {
            retval += " " + context.getString(R.string.DaysAgo);
        }
        /*
                if (movieAge == 0)
                {
                    retval += context.getString(R.string.Today);
                }
                else if (movieAge == 1)
                {
                    retval += context.getString(R.string.Yesterday);
                }
                else
                {
                    retval += movieAge;
                    retval += " " + context.getString(R.string.DaysAgo);
                }
        */
        return retval;
    }

    private class ViewHolder
    {
        TextView  movieNameText;
        TextView  rentalDateText;
        TextView  rentalAgeText;
        Button    returnButton;
        ImageView movieImage;
        ImageView returnedImage;
        Movie     movie;
        float     initialTextSize;
    }

}
