package com.spekisoftware.RTM;

import android.app.Application;

public class ReminderApplication extends Application
{
    private static ReminderApplication instance;
    
    public ReminderApplication()
    {
        instance = this;
    }
    
    public static ReminderApplication getContext()
    {
        return instance;
    }

}
