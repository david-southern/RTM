package com.spekisoftware.RTM;

import android.util.Log;

public class Logger
{
    public static final int LOG_LEVEL_ERROR = 10;
    public static final int LOG_LEVEL_WARN = 30;
    public static final int LOG_LEVEL_INFO = 50;
    public static final int LOG_LEVEL_DEBUG = 70;

    private static final boolean allowLogging = false;
    
    public static void Log(int LOG_LEVEL, String logTag, String formatString, Object ... formatParams)
    {
        if(allowLogging)
        {            
            String userMessage;
            
            if(formatParams == null || formatParams.length < 1)
            {
                userMessage = formatString;    
            }
            else
            {
                userMessage = String.format(formatString, formatParams);                
            }
            
            String logMessage = String.format("TID: %d - %s", Thread.currentThread().getId(), userMessage);
            
            if(LOG_LEVEL <= LOG_LEVEL_ERROR)
            {
                Log.e(logTag, logMessage);
            }
            else if(LOG_LEVEL <= LOG_LEVEL_WARN)
            {
                Log.w(logTag, logMessage);
            }
            else if(LOG_LEVEL <= LOG_LEVEL_INFO)
            {
                Log.i(logTag, logMessage);
            }
            else if(LOG_LEVEL <= LOG_LEVEL_DEBUG)
            {
                Log.d(logTag, logMessage);
            }
            else                
            {
                Log.v(logTag, logMessage);
            }
        }
    }

}
