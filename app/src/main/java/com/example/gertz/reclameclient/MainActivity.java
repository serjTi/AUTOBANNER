package com.example.gertz.reclameclient;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.gertz.reclameclient.AsyncTasks.AuthResponse;
import com.example.gertz.reclameclient.AsyncTasks.AuthenticationTask;
import com.example.gertz.reclameclient.AsyncTasks.LogoutResponse;
import com.example.gertz.reclameclient.AsyncTasks.LogoutTask;
import com.example.gertz.reclameclient.AsyncTasks.SendReportResponse;
import com.example.gertz.reclameclient.AsyncTasks.SendReportTask;
import com.example.gertz.reclameclient.data.Constants;
import com.example.gertz.reclameclient.fragments.AuthFragment;
import com.example.gertz.reclameclient.fragments.GPSFragment;
import com.example.gertz.reclameclient.fragments.MemorySettings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements AuthFragment.IOnMyAuthClickListener,
        GPSFragment.IOnMyGPSClickListener, AuthResponse, SendReportResponse, LogoutResponse {

    File photoFile;
    FragmentTransaction fragmentTransaction;
    private static GPSFragment gpsFragment = new GPSFragment();
    private static AuthFragment authFragment = new AuthFragment();

    private GPSTracker gpsTracker = null;
    private boolean mIsBound;

    //settings
    DialogFragment memory;
    public static String memoryPreferences = "";
    SharedPreferences sharedPref;
    SharedPreferences tokenPrefs;
    int distance = 0;
    String accessToken = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        memory settings
        sharedPref = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        if (sharedPref.contains(Constants.KEY_MEMORY_PLACE)) {
            memoryPreferences = sharedPref.getString(Constants.KEY_MEMORY_PLACE, "");
            if (memoryPreferences.equals("sd")) {
                Log.d("TAG", "sharedPref = " + memoryPreferences);
                String state = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    Log.d(Constants.TAG, "ExternalStorageState = " + state);
                }
            }
        }

        //       ACCESS TOKEN ENABLED
       tokenPrefs = getSharedPreferences(Constants.TOKEN_PREFERENCES, Context.MODE_PRIVATE);
        if (tokenPrefs.contains(Constants.ACCESS_TOKEN)) {
            accessToken = tokenPrefs.getString(Constants.ACCESS_TOKEN, "");
                Log.d("TAG", "tokenPrefs = " + accessToken);
            fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragmentContainer, gpsFragment);
            fragmentTransaction.commit();
        } else {
            authFragment = new AuthFragment();
            fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragmentContainer, authFragment);
            fragmentTransaction.commit();
        }


    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onAuthButtonClick(String l, String p) {
        new AuthenticationTask(this).execute(l, p);


    }


    @Override
    public void onStartGPSButtonClick() {
        startService(new Intent(this.getBaseContext(), GPSTracker.class));
        doBindService();
    }

    @Override
    public void onStopGPSButtonClick() {
        doUnbindService();
        stopService(new Intent(this.getBaseContext(), GPSTracker.class));

    }

    @Override
    public void onReportButtonClick() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, Constants.CAMERA_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");

            // Base64 bitmapCode
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
            sendReport(encoded, accessToken, 15);

            File photoBase64Code = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "photoBase64Code" + ".txt");
            FileOutputStream outputStreamTXT = null;
            try {
                outputStreamTXT = new FileOutputStream(photoBase64Code);
                photo.compress(Bitmap.CompressFormat.JPEG, 100, outputStreamTXT);
                outputStreamTXT.flush();
                outputStreamTXT.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e(Constants.TAG, "FileNotFoundException = " + e.toString());
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(Constants.TAG, "IOException = " + e.toString());
            }
            Log.d(Constants.TAG, "encodedPhoto = <<<" + encoded + ">>>");

            //saving photoFile
            photoFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), new java.util.Date().toString() + ".jpg");
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(photoFile);
                photo.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                outputStream.flush();
                outputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e(Constants.TAG, "FileNotFoundException = " + e.toString());
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(Constants.TAG, "IOException = " + e.toString());
            }
        }
    }

    private void sendReport(String encoded, String token, int distance) {
        String dst = String.valueOf(distance);
        Log.d(Constants.TAG, "dst = " + dst);
        new SendReportTask(this).execute(encoded, token, dst);

    }

    private Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message message) {

            if (message.what == Constants.DISTANCE_UPDATED) {
                distance = (int) message.getData().getFloat(Constants.DISTANCE);
                Log.d(Constants.TAG, "MA distance = " + distance);
                gpsFragment.setDistance(distance);
            } else if (message.what == Constants.GPS_DISABLED) {
                startActivity(new Intent(
                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } else if (message.what == Constants.CITY_LOCALITY) {
                String locality = message.getData().getString(Constants.LOCALITY);
                gpsFragment.setLocality(locality);
            } else if (message.what == Constants.OUT_OF_TOWN_LOCALITY) {
                String locality = message.getData().getString(Constants.LOCALITY);
                gpsFragment.setLocality(locality);
            }
            return true;
        }

    });
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            gpsTracker = ((GPSTracker.LocalBinder) iBinder).getInstance();
            gpsTracker.setHandler(handler);

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            gpsTracker = null;
        }
    };

    private void doBindService() {
        // Establish counter connection with the service.  We use an explicit
        // class name because we want counter specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(this,
                GPSTracker.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    private void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                memory = new MemorySettings();
                memory.show(getFragmentManager(), "memory");
                return true;

            case R.id.action_exit:
                new LogoutTask(this).execute(accessToken);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void authIsDone(String output) {
Log.d(Constants.TAG, "MA auth response = <<<" + output + ">>>");
        //MA auth response = <<<{"status":"success","access_token":"WCgFnqR69qdiWqBxAofbibAahlPWP2P0"}>>>
        JSONObject jsAuth = null;
        try {
            jsAuth = new JSONObject(output);
            String statusAuth = jsAuth.getString(Constants.STATUS);
            Log.d(Constants.TAG, "status = " + statusAuth);
            if (statusAuth.equals("success")){

                fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.fragmentContainer, gpsFragment);
                fragmentTransaction.commit();

                accessToken = jsAuth.getString(Constants.ACCESS_TOKEN);
                Log.d(Constants.TAG, "status = " + statusAuth + "; accessToken = " + accessToken);
                savePreferences(Constants.ACCESS_TOKEN ,accessToken);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void savePreferences(String key, String value) {
        SharedPreferences sharedPreferences = getBaseContext().getSharedPreferences(
                Constants.TOKEN_PREFERENCES, MainActivity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    @Override
    public void sendReportResponse(String reportResponse) {
        Log.d(Constants.TAG, "MA reportResponse = <<<" + reportResponse + ">>>");
    }

    @Override
    public void logoutComplete(String outResponse) {
        Log.d(Constants.TAG, "MA Logout Response = <<<" + outResponse + ">>>");
        JSONObject jLogout = null;
        try {
            jLogout = new JSONObject(outResponse);
            String logoutStatus = jLogout.getString(Constants.STATUS);
            Log.d(Constants.TAG, "logoutStatus = " +logoutStatus);

            //       DELETE TOKEN
            tokenPrefs = getSharedPreferences(Constants.TOKEN_PREFERENCES, Context.MODE_PRIVATE);
            if (tokenPrefs.contains(Constants.ACCESS_TOKEN)) {
                tokenPrefs.edit().clear().commit();
                if (tokenPrefs.getString(Constants.ACCESS_TOKEN, "").equals("")){
                    Log.d(Constants.TAG, "current access_token deleted");
                    authFragment = new AuthFragment();
                    fragmentTransaction = getSupportFragmentManager().beginTransaction();
                    fragmentTransaction.replace(R.id.fragmentContainer, authFragment);
                    fragmentTransaction.commit();
                }

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}