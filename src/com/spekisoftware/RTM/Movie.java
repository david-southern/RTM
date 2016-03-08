package com.spekisoftware.RTM;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Movie
{
    private int              id;
    private String           name;
    private Date             rentalDate;
    private String           rentalSource;
    private String           imageURL;
    private long             undoReturnTimestamp;
    private transient Bitmap image;

    public Movie(String _name, Date _rentalDate, String _rentalSource)
    {
        name = _name;
        setRentalDate(_rentalDate);
        rentalSource = _rentalSource;
        imageURL = null;
        image = null;
        undoReturnTimestamp = 0;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof Movie)) { return false; }

        Movie other = (Movie)o;

        return id == other.id;
    }

    @Override
    public int hashCode()
    {
        return id;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Date getRentalDate()
    {
        return rentalDate;
    }

    public void setRentalDate(Date rentalDate)
    {
        // Truncate the rental Date to midnight AM of the rental day so that the first day of notification will be the
        // day after the rental, regardless of the relative position of the rental time and the notification time
        Calendar rentalTime = Calendar.getInstance();
        
        rentalTime.setTimeInMillis(rentalDate.getTime());

        rentalTime.set(Calendar.HOUR_OF_DAY, 0);
        rentalTime.set(Calendar.MINUTE, 0);
        rentalTime.set(Calendar.SECOND, 0);
        rentalTime.set(Calendar.MILLISECOND, 0);

        this.rentalDate = rentalTime.getTime();
    }

    public String getRentalSource()
    {
        return rentalSource;
    }

    public void setRentalSource(String rentalSource)
    {
        this.rentalSource = rentalSource;
    }

    public long getUndoRentalTimestamp()
    {
        return this.undoReturnTimestamp;
    }

    public void setUndoRentalTimestamp(long undoReturnTimestamp)
    {
        this.undoReturnTimestamp = undoReturnTimestamp;
    }

    public String getImageURL()
    {
        return this.imageURL;
    }

    public void setImageURL(String imageURL)
    {
        this.imageURL = imageURL;
    }

    public Bitmap getImage()
    {
        if (image == null && imageURL != null)
        {
            loadImage();
        }
        return this.image;
    }

    public void setImage(Bitmap image)
    {
        this.image = image;
    }

    public int getAge()
    {
        Calendar today = Calendar.getInstance();
        Calendar rentedOn = Calendar.getInstance();
        rentedOn.setTime(rentalDate);

        if (today.before(rentedOn)) { return 0; }

        int daysBetween = 0;

        while (rentedOn.before(today))
        {
            rentedOn.add(Calendar.DAY_OF_MONTH, 1);
            daysBetween++;
        }

        return daysBetween - 1;
    }

    private String getImageFilename()
    {
        if (imageURL == null) { return null; }

        int lastSlashIndex = imageURL.lastIndexOf("/");

        if (lastSlashIndex == -1) { return null; }

        return imageURL.substring(lastSlashIndex + 1);
    }

    public void loadImage()
    {
        if (imageURL == null) { return; }

        try
        {
            FileInputStream fis = ReminderApplication.getContext().openFileInput(getImageFilename());

            image = BitmapFactory.decodeStream(fis);

            Logger.Log(Logger.LOG_LEVEL_INFO, "Movie.loadImage", String.format("Loaded image %s", getImageFilename()));
        }
        catch (FileNotFoundException ex)
        {
            image = null;
            Logger.Log(Logger.LOG_LEVEL_INFO, "Movie.loadImage",
                    String.format("Unable to load image: %s", getImageFilename()));
        }
    }

    public void saveImage()
    {
        if (image == null || imageURL == null) { return; }

        try
        {
            FileOutputStream fos = ReminderApplication.getContext().openFileOutput(getImageFilename(),
                    Context.MODE_PRIVATE);

            image.compress(Bitmap.CompressFormat.PNG, 100, fos);

            Logger.Log(Logger.LOG_LEVEL_INFO, "Movie.saveImage", String.format("Saved image %s", getImageFilename()));
        }
        catch (FileNotFoundException ex)
        {
            Logger.Log(Logger.LOG_LEVEL_ERROR, "Movie.saveImage",
                    String.format("Caught FileNotFound exception for image %s: %s", getImageFilename(), ex.toString()));
        }
    }

    public void deleteImage()
    {
        if (imageURL == null) { return; }

        Logger.Log(Logger.LOG_LEVEL_INFO, "Movie.deleteImage", String.format("Deleting image %s", getImageFilename()));
        ReminderApplication.getContext().deleteFile(getImageFilename());
    }

}
