package com.example.gertz.reclameclient.AsyncTasks;

import android.os.AsyncTask;
import android.util.Log;

import com.example.gertz.reclameclient.data.Constants;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LogoutTask extends AsyncTask<String, Void, String> {

    private LogoutResponse delegate = null;

    public LogoutTask(LogoutResponse listener) {
        delegate = listener;
    }

    @Override
    protected String doInBackground(String... params) {
        StringBuffer buffer = new StringBuffer();
        HttpClient client = new DefaultHttpClient();
        HttpPost httpPost;
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);

        try {
            nameValuePairs.add(new BasicNameValuePair(Constants.ACCESS_TOKEN, params[0]));
            httpPost = new HttpPost(Constants.LOGOUT_URL);
            httpPost.addHeader(Constants.AUTHENTICATION, Constants.AUTHENTICATION_KEY);
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
            HttpResponse response = client.execute(httpPost);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                return buffer.toString();
            } else {
                Log.e("Tag", "Error in authentication request");
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            String ee = e.toString();
            Log.e("Error", ee);
        } catch (IOException e) {
            e.printStackTrace();
            String ee = e.toString();
            Log.e("Error", ee);
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (delegate != null)
            delegate.logoutComplete(result);
    }


}
