package com.ctext.objectdetection;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ObjectDefinitionAsyncTask extends AsyncTask<String, Void, JSONObject> {
    // Request example: curl --header "Authorization: Token 61f80afba945a78ae5d103ad5a3f616ee6c4def5" https://owlbot.info/api/v4/dictionary/owl -s | json_pp
    private String TAG = "ObjectDefinitionAsyncTask";
    private String token = "Token 61f80afba945a78ae5d103ad5a3f616ee6c4def5"; // Using https://owlbot.info/ API
    private String api = "https://owlbot.info/api/v4/dictionary/";

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected JSONObject doInBackground(String... word) {
        try {
            URL url = new URL(api + word[0]);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestProperty("Authorization", token);
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.connect();

            int statusCode = httpURLConnection.getResponseCode();
            if (statusCode ==  200) {
                InputStream inputStream = new BufferedInputStream(httpURLConnection.getInputStream());

                String response = convertInputStreamToString(inputStream);
                Log.d(TAG, response);
                if (!response.isEmpty()) {
                    JSONObject jsonObject = new JSONObject(response);
                    return jsonObject;
                }
            } else {
                Log.d(TAG, "Status code not success: " + statusCode);
                return null;
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /* From https://medium.com/@lewisjkl/android-httpurlconnection-with-asynctask-tutorial-7ce5bf0245cd */
    private String convertInputStreamToString(InputStream inputStream) {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

}
