package com.spekisoftware.RTM;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;

public class RESTHandler extends AsyncTask<String, String, String>
{
    public interface RESTCallback
    {
        public void callback(RESTResponse message);
    }

    public class RESTResponse
    {
        public boolean isError          = false;
        public String  exceptionMessage = null;
        public int     statusCode       = -1;
        public String  statusMessage    = null;
        public String  responseBody     = null;
    }

    public RESTCallback  onSuccess = null;
    public RESTCallback  onError   = null;

    private RESTResponse result;

    @Override
    protected String doInBackground(String... uri)
    {
        final String LOGTAG = "RESTHandler";

        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;

        result = new RESTResponse();

        if(uri == null || uri[0] == null)
        {
            result.isError = true;
            result.exceptionMessage = "NULL URI passed to RESTHandler";
            Logger.Log(Logger.LOG_LEVEL_ERROR, LOGTAG, String.format("RESTHandler: Received NULL URI parameter"));
            return null;
        }
        
        try
        {
            Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format("RESTHandler: Loading URL: %s", uri[0]));

            response = httpclient.execute(new HttpGet(uri[0]));
            
            StatusLine statusLine = response.getStatusLine();

            Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                    String.format("RESTHandler: Got %s status, downloading response string",
                            statusLine.getReasonPhrase()));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            out.close();

            result.responseBody = out.toString();
            result.statusCode = statusLine.getStatusCode();
            result.statusMessage = statusLine.getReasonPhrase();

            if (statusLine.getStatusCode() != HttpStatus.SC_OK)
            {
                result.isError = true;
                result.exceptionMessage = result.responseBody;
            }

        }
        catch (ClientProtocolException e)
        {
            Logger.Log(Logger.LOG_LEVEL_ERROR, LOGTAG, String.format("RESTHandler: Caught client protocol exception: %s", e.toString()));
            result.isError = true;
            result.exceptionMessage = e.getMessage();
        }
        catch (IOException e)
        {
            Logger.Log(Logger.LOG_LEVEL_ERROR, LOGTAG, String.format("RESTHandler: Caught IOException: %s", e.toString()));
            result.isError = true;
            result.exceptionMessage = e.getMessage();
        }
        catch (Exception e)
        {
            Logger.Log(Logger.LOG_LEVEL_ERROR, LOGTAG, String.format("RESTHandler: Caught Unknown Exception: %s", e.toString()));
            result.isError = true;
            result.exceptionMessage = e.getMessage();
        }
        
        Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format("RESTHandler: returning %d bytes", result.responseBody.length()));
        return null;
    }

    @Override
    protected void onPostExecute(String dummy)
    {
        super.onPostExecute(dummy);

        if (result.isError)
        {
            if (onError != null)
            {
                onError.callback(result);
            }
        }
        else
        {
            if (onSuccess != null)
            {
                onSuccess.callback(result);
            }
        }
    }
}