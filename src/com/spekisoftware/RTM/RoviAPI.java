package com.spekisoftware.RTM;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Hashtable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class RoviAPI
{
    private final static String ROVI_API_KEY       = "my8hmpfcw5usxdpmfnzkgbvz";
    private final static String ROVI_SHARED_SECRET = "cNTZfyAGFK";

    private final static String APIKEY_TOKEN       = "{{APIKEY}}";
    private final static String SIGNATURE_TOKEN    = "{{SIG}}";
    private final static String QUERY_TOKEN        = "{{QUERY}}";

    private final static String AUTOCOMPLETE_URL   = "http://api.rovicorp.com/search/v2/amgvideo/autocomplete?apikey="
                                                           + APIKEY_TOKEN + "&sig=" + SIGNATURE_TOKEN
                                                           + "&entitytype=movie&size=10&format=json&query="
                                                           + QUERY_TOKEN;

    private String GenerateSig()
    {
        String sig = ROVI_API_KEY;
        sig += ROVI_SHARED_SECRET;
        sig += (System.currentTimeMillis() / 1000);
        return Utils.GetMD5Hash(sig);
    }

    public interface AutocompleteCallback
    {
        public void callback(ArrayList<String> result);
    }

    public void AutocompleteMovies(String queryString, final AutocompleteCallback onSuccess,
            RESTHandler.RESTCallback onError)
    {
        if (onSuccess == null) { return; }

        final String LOGTAG = "AutocompleteMovies";

        Hashtable<String, String> tokens = new Hashtable<String, String>();

        tokens.put(APIKEY_TOKEN, ROVI_API_KEY);
        tokens.put(SIGNATURE_TOKEN, GenerateSig());
        tokens.put(QUERY_TOKEN, URLEncoder.encode(queryString));

        String requestUrl = Utils.ReplaceTokens(AUTOCOMPLETE_URL, tokens);

        RESTHandler handler = new RESTHandler();

        handler.onSuccess = new RESTHandler.RESTCallback()
        {
            @Override
            public void callback(RESTHandler.RESTResponse result)
            {
                Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format("Got success from RESTHandler, payload: %s", result.responseBody));

                RoviAutocompleteDTO resultObj;

                Type collectionType = new TypeToken<RoviAutocompleteDTO>()
                {
                }.getType();
                resultObj = new Gson().fromJson(result.responseBody, collectionType);

                ArrayList<String> movieList = new ArrayList<String>();

                if (resultObj != null && resultObj.autocompleteResponse != null
                        && resultObj.autocompleteResponse.results != null)
                {
                    for (String movie : resultObj.autocompleteResponse.results)
                    {
                        movieList.add(movie);
                    }
                }

                Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format("Calling Autocomplete success callback with %d movies", movieList.size()));
                onSuccess.callback(movieList);
            }
        };

        handler.onError = onError;

        Logger.Log(Logger.LOG_LEVEL_INFO, LOGTAG, String.format("Calling Rovi API with request URL: %s", requestUrl));

        handler.execute(requestUrl);
    }

    @SuppressWarnings("unused")
    private class RoviMessage
    {
        public String code;
        public String name;
    }

    @SuppressWarnings("unused")
    private class RoviControlSet
    {
        String        status;
        int           code;
        RoviMessage[] messages;
    }

    public class ACResponse
    {
        String                id;
        public RoviControlSet controlSet;
        String[]              results;
    }

    private class RoviAutocompleteDTO
    {
        public ACResponse autocompleteResponse;
    }
}
