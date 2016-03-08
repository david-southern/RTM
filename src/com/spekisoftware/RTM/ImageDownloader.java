package com.spekisoftware.RTM;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

public class ImageDownloader extends AsyncTask<String, String, String>
{
    public interface SuccessCallback
    {
        public void callback(Bitmap result);
    }

    public interface ErrorCallback
    {
        public void callback(ErrorResponse result);
    }

    public class ErrorResponse
    {
        public boolean isError          = false;
        public String  exceptionMessage = null;
        public int     statusCode       = -1;
        public String  statusMessage    = null;
        public String  responseBody     = null;
    }

    public SuccessCallback onSuccess = null;
    public ErrorCallback   onError   = null;

    private Bitmap         bitmapResult;
    private ErrorResponse  errorResult;

    @Override
    protected String doInBackground(String... uri)
    {
        final String LOGTAG = "ImageDownloader";

        errorResult = new ErrorResponse();

        try
        {
            if(uri == null || uri.length < 1 || uri[0] == null || uri[0].trim().length() < 1)
            {
                throw new Exception("ImageDownloader: No URL passed as input");
            }
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;

            Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format("ImageDownloader: Loading URL: %s", uri[0]));

            response = httpclient.execute(new HttpGet(uri[0]));
            StatusLine statusLine = response.getStatusLine();

            Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                    String.format("ImageDownloader: Got %s status, downloading response string",
                            statusLine.getReasonPhrase()));

            errorResult.responseBody = null;
            errorResult.statusCode = statusLine.getStatusCode();
            errorResult.statusMessage = statusLine.getReasonPhrase();

            bitmapResult = BitmapFactory.decodeStream(response.getEntity().getContent());
            
            if (statusLine.getStatusCode() != HttpStatus.SC_OK)
            {
                errorResult.isError = true;
                errorResult.exceptionMessage = errorResult.statusMessage;
            }

        }
        catch (ClientProtocolException e)
        {
            Logger.Log(Logger.LOG_LEVEL_ERROR, LOGTAG, String.format("RESTHandler: Caught client protocol exception: %s", e.toString()));
            errorResult.isError = true;
            errorResult.exceptionMessage = e.getMessage();
        }
        catch (IOException e)
        {
            Logger.Log(Logger.LOG_LEVEL_ERROR, LOGTAG, String.format("RESTHandler: Caught IOException: %s", e.toString()));
            errorResult.isError = true;
            errorResult.exceptionMessage = e.getMessage();
        }
        catch (Exception e)
        {
            Logger.Log(Logger.LOG_LEVEL_ERROR, LOGTAG, String.format("RESTHandler: Caught Exception: %s", e.toString()));
            errorResult.isError = true;
            errorResult.exceptionMessage = e.getMessage();
        }

        Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG,
                String.format("RESTHandler: returning (%d x %d) image",
                        bitmapResult == null ? -1 : bitmapResult.getWidth(),
                        bitmapResult == null ? -1 : bitmapResult.getHeight()));
        return null;
    }

    @Override
    protected void onPostExecute(String dummy)
    {
        super.onPostExecute(dummy);

        if (errorResult.isError)
        {
            if (onError != null)
            {
                onError.callback(errorResult);
            }
        }
        else
        {
            if (onSuccess != null)
            {
                onSuccess.callback(bitmapResult);
            }
        }
    }
}