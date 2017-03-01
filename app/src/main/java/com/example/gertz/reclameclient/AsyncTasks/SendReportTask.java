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

public class SendReportTask extends AsyncTask<String, Void, String> {

    private SendReportResponse delegate = null;

    public SendReportTask(SendReportResponse listener) {
        delegate = listener;
    }

    @Override
    protected String doInBackground(String... params) {
        Log.d(Constants.TAG, Constants.ENCODED_PHOTO + " = " + params[0] );
        Log.d(Constants.TAG, Constants.ACCESS_TOKEN + " = " + params[1] );
        Log.d(Constants.TAG, Constants.DISTANCE + " = " + params[2] );
        StringBuffer buffer = new StringBuffer();
        HttpClient client = new DefaultHttpClient();
        HttpPost httpPost;
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        try {
            // encoded, accessToken, dst
            nameValuePairs.add(new BasicNameValuePair(Constants.ENCODED_PHOTO, params[0]));
            nameValuePairs.add(new BasicNameValuePair(Constants.ACCESS_TOKEN, params[1]));
            nameValuePairs.add(new BasicNameValuePair(Constants.DISTANCE, params[2]));
            httpPost = new HttpPost(Constants.REPORT_URL);
            httpPost.addHeader(Constants.AUTHENTICATION, Constants.AUTHENTICATION_KEY);
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
            HttpResponse response = client.execute(httpPost);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            Log.d(Constants.TAG, "Send Report status code" + String.valueOf(statusCode));
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
                Log.e("Tag", "Error in SendReportTask");
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            if (delegate != null)
                delegate.sendReportResponse(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            if (delegate != null)
                delegate.sendReportResponse(e.getMessage());
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (delegate != null)
            delegate.sendReportResponse(s);
    }


}
