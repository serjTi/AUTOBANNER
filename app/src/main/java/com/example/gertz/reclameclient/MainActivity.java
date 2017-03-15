package com.example.gertz.reclameclient;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.example.gertz.reclameclient.AsyncTasks.AuthResponse;
import com.example.gertz.reclameclient.AsyncTasks.AuthenticationTask;
import com.example.gertz.reclameclient.AsyncTasks.LogoutResponse;
import com.example.gertz.reclameclient.AsyncTasks.LogoutTask;
import com.example.gertz.reclameclient.AsyncTasks.SendReportResponse;
import com.example.gertz.reclameclient.AsyncTasks.SendReportTask;
import com.example.gertz.reclameclient.data.Constants;
import com.example.gertz.reclameclient.data.Csensors;
import com.example.gertz.reclameclient.fragments.AuthFragment;
import com.example.gertz.reclameclient.fragments.GPSFragment;
import com.example.gertz.reclameclient.fragments.MemorySettings;
import com.example.gertz.reclameclient.services.DetectedActivitiesIntentService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionApi;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import es.dmoral.toasty.Toasty;

public class MainActivity extends AppCompatActivity implements AuthFragment.IOnMyAuthClickListener,
        GPSFragment.IOnMyGPSClickListener, GPSFragment.IOnMyGPSLongClickListener, AuthResponse, SendReportResponse, LogoutResponse,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback<Status> {


    // Objects
    protected ActivityDetectionBroadcastReceiver mBroadcastReceiver;
    private ArrayList<DetectedActivity> mDetectedActivities;
    File photoFile;
    FragmentTransaction fragmentTransaction;
    private static GPSFragment gpsFragment = new GPSFragment();
    private static AuthFragment authFragment = new AuthFragment();
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    DialogFragment memory;
    SharedPreferences sharedPref;
    SharedPreferences tokenPrefs;
    SharedPreferences currentDstPrefs;
    Location location_A;


    // Strings
    public static String login = "";
    public static String memoryPreferences = "";
    private String accessToken = "";


    // Counters
    int distance = 0;
    float curDst = 0;
    int counter = 0;
    double totalDST = 0;

    // F L A G S
    private boolean isActivityDetectionEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
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

        //       GET CURRENT DISTANCE
        currentDstPrefs = getSharedPreferences(Constants.CURRENT_DISTANCE, Context.MODE_PRIVATE);
        if (currentDstPrefs.contains(Constants.CURRENT_DISTANCE)) {
            curDst = Float.parseFloat(currentDstPrefs.getString(Constants.CURRENT_DISTANCE, ""));
            curDst = curDst + distance;
            Log.d("TAG", "curDST = " + curDst);
            gpsFragment.setDistance(curDst);
        }
        if (savedInstanceState != null && savedInstanceState.containsKey(
                Csensors.DETECTED_ACTIVITIES)) {
            mDetectedActivities = (ArrayList<DetectedActivity>) savedInstanceState.getSerializable(
                    Csensors.DETECTED_ACTIVITIES);
        } else {
            mDetectedActivities = new ArrayList<DetectedActivity>();

            // Set the confidence level of each monitored activity to zero.
            for (int i = 0; i < Csensors.MONITORED_ACTIVITIES.length; i++) {
                mDetectedActivities.add(new DetectedActivity(Csensors.MONITORED_ACTIVITIES[i], 0));
            }
        }
        mBroadcastReceiver = new ActivityDetectionBroadcastReceiver();
// Kick off the request to build GoogleApiClient.
        buildGoogleApiClient();
    }


    @Override
    public void onAuthButtonClick(String l, String p) {
        new AuthenticationTask(this).execute(l, p);
        login = l;
    }


    @Override
    public void onStartGPSButtonClick() {
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        } else{
            return;
        }


//        if (isServiceEnabled) {
//            doBindService();
//        } else {
//            startGpsService();
//            doBindService();
//        }
    }

    @Override
    public void onStopGPSButtonClick() {
        if (mGoogleApiClient.isConnected()){
            removeActivityUpdatesButtonHandler();
            mGoogleApiClient.disconnect();
            Log.d(Constants.TAG, "isGoogleConnected = "+ mGoogleApiClient.isConnected());
        }else{
            return;
        }
//        gpsFragment.setDistance(0);
//        doUnbindService();
//        stopService(new Intent(this.getBaseContext(), GPSTracker.class));
//        isServiceEnabled = false;
//        Log.d(Constants.TAG, "isServiceEnabled = " + isServiceEnabled);

    }

    @Override
    public void onReportButtonClick() {
        gpsFragment.setGPSOffBtn();
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, Constants.CAMERA_REQUEST);
    }

    @Override
    public void onSettingsButtonClick() {

    }

    @Override
    public void onLogOutButtonClick() {
        new LogoutTask(this).execute(accessToken);
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
            String todaysDate = new java.util.Date().toString();
            photoFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), todaysDate + ".jpg");
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
        SharedPreferences sharedPreferences = getBaseContext().getSharedPreferences(
                Constants.TOKEN_PREFERENCES, MainActivity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.CURRENT_DISTANCE, String.valueOf(distance));
        editor.commit();
    }

    private void sendReport(String encoded, String token, int distance) {
        String dst = String.valueOf(distance);
        Log.d(Constants.TAG, "dst = " + dst);
        new SendReportTask(this).execute(encoded, token, dst);

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
        if (output != null) {
            JSONObject jsAuth = null;
            try {
                jsAuth = new JSONObject(output);
                String statusAuth = jsAuth.getString(Constants.STATUS);
                Log.d(Constants.TAG, "status = " + statusAuth);
                if (statusAuth.equals("success")) {
                    Toasty.success(this, "Добро пожаловать!", Toast.LENGTH_SHORT).show();
                    fragmentTransaction = getSupportFragmentManager().beginTransaction();
                    fragmentTransaction.replace(R.id.fragmentContainer, gpsFragment);
                    fragmentTransaction.commit();
                    accessToken = jsAuth.getString(Constants.ACCESS_TOKEN);
                    Log.d(Constants.TAG, "status = " + statusAuth + "; accessToken = " + accessToken);
                    savePreferences(Constants.ACCESS_TOKEN, accessToken);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Toasty.error(this, "Authentication Error", Toast.LENGTH_SHORT).show();
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

        if (reportResponse != null) {
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(reportResponse);
                String respStatus = jsonObject.getString(Constants.STATUS);
                if (respStatus.equals("success")) {
                    Toasty.success(this, "Отчёт отправлен!", Toast.LENGTH_SHORT, true).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Toasty.error(this, "Report Request Error", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void logoutComplete(String outResponse) {
        Log.d(Constants.TAG, "MA Logout Response = <<<" + outResponse + ">>>");
        if (outResponse == null) {
            Toasty.error(this, "Log Out Error", Toast.LENGTH_SHORT).show();
        } else {
            JSONObject jLogout = null;
            try {
                jLogout = new JSONObject(outResponse);
                String logoutStatus = jLogout.getString(Constants.STATUS);
                Log.d(Constants.TAG, "logoutStatus = " + logoutStatus);
                if (logoutStatus.equals(Constants.SUCCESS)) {
                    Toasty.success(this, "Log out " + Constants.SUCCESS, Toast.LENGTH_SHORT).show();
                    //       DELETE TOKEN
                    tokenPrefs = getSharedPreferences(Constants.TOKEN_PREFERENCES, Context.MODE_PRIVATE);
                    if (tokenPrefs.contains(Constants.ACCESS_TOKEN)) {
                        tokenPrefs.edit().clear().commit();
                        if (tokenPrefs.getString(Constants.ACCESS_TOKEN, "").equals("")) {
                            Log.d(Constants.TAG, "current access_token deleted");
                            authFragment = new AuthFragment();
                            fragmentTransaction = getSupportFragmentManager().beginTransaction();
                            fragmentTransaction.replace(R.id.fragmentContainer, authFragment);
                            fragmentTransaction.commit();
                        }

                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleNewLocation(Location location) {
        float acccuracy = location.getAccuracy();
        counter++;
        Log.d(Constants.TAG, "location " + counter + "; Accuracy = " + location.getAccuracy());
        if (location_A == null) {
            location_A = location;
        } else {
            if (acccuracy < Constants.GPS_LOCATION_ACCURACY) {
                totalDST += location.distanceTo(location_A);
                location_A = location;
                gpsFragment.setDistance(totalDST);
            } else {
                Log.d(Constants.TAG, "distance does't change");
                location_A = location;
            }
        }

    }


    @Override
    public void onConnected(Bundle bundle) {
        Log.d(Constants.TAG, "гугл коннектед");
        Location startLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        requestActivityUpdatesButtonHandler();
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
/*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, Constants.CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Log.i(Constants.TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * ActivityRecognition API.
     */
    public void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .addApi(LocationServices.API)
                .build();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();


        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the broadcast receiver that informs this activity of the DetectedActivity
        // object broadcast sent by the intent service.
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter(Csensors.BROADCAST_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(Constants.TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    /**
     * Registers for activity recognition updates using
     * {@link com.google.android.gms.location.ActivityRecognitionApi#requestActivityUpdates} which
     * returns a {@link com.google.android.gms.common.api.PendingResult}. Since this activity
     * implements the PendingResult interface, the activity itself receives the callback, and the
     * code within {@code onResult} executes. Note: once {@code requestActivityUpdates()} completes
     * successfully, the {@code DetectedActivitiesIntentService} starts receiving callbacks when
     * activities are detected.
     */
    public void requestActivityUpdatesButtonHandler() {
        Log.d(Constants.TAG, "requestActivityUpdatesButtonHandler");
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected),
                    Toast.LENGTH_SHORT).show();
            isActivityDetectionEnabled = false;
            return;
        }
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                mGoogleApiClient,
                Csensors.DETECTION_INTERVAL_IN_MILLISECONDS,
                getActivityDetectionPendingIntent()
        ).setResultCallback(this);
        isActivityDetectionEnabled = true;
    }

    /**
     * Removes activity recognition updates using
     * {@link com.google.android.gms.location.ActivityRecognitionApi#removeActivityUpdates} which
     * returns a {@link com.google.android.gms.common.api.PendingResult}. Since this activity
     * implements the PendingResult interface, the activity itself receives the callback, and the
     * code within {@code onResult} executes. Note: once {@code removeActivityUpdates()} completes
     * successfully, the {@code DetectedActivitiesIntentService} stops receiving callbacks about
     * detected activities.
     */
    public void removeActivityUpdatesButtonHandler() {
        Log.d(Constants.TAG, "removeActivityUpdatesButtonHandler");
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            isActivityDetectionEnabled = false;
            return;
        }
        // Remove all activity updates for the PendingIntent that was used to request activity
        // updates.
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
                mGoogleApiClient,
                getActivityDetectionPendingIntent()
        ).setResultCallback(this);
        isActivityDetectionEnabled = false;

    }

    /**
     * Runs when the result of calling requestActivityUpdates() and removeActivityUpdates() becomes
     * available. Either method can complete successfully or with an error.
     *
     * @param status The Status returned through a PendingIntent when requestActivityUpdates()
     *               or removeActivityUpdates() are called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {
            // Toggle the status of activity updates requested, and save in shared preferences.
            boolean requestingUpdates = !getUpdatesRequestedState();
            setUpdatesRequestedState(requestingUpdates);

            // Update the UI. Requesting activity updates enables the Remove Activity Updates
            // button, and removing activity updates enables the Add Activity Updates button.
            setButtonsEnabledState();

            Toasty.success(
                    this,
                    getString(requestingUpdates ? R.string.activity_updates_added :
                            R.string.activity_updates_removed),
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            Log.e(Constants.TAG, "Error adding or removing activity detection: " + status.getStatusMessage());
        }
    }

    /**
     * Gets a PendingIntent to be sent for each activity detection.
     */
    private PendingIntent
    getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, DetectedActivitiesIntentService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Ensures that only one button is enabled at any time. The Request Activity Updates button is
     * enabled if the user hasn't yet requested activity updates. The Remove Activity Updates button
     * is enabled if the user has requested activity updates.
     */
    private void setButtonsEnabledState() {
        if (getUpdatesRequestedState()) {
//            mRequestActivityUpdatesButton.setEnabled(false);
//            mRemoveActivityUpdatesButton.setEnabled(true);
        } else {
//            mRequestActivityUpdatesButton.setEnabled(true);
//            mRemoveActivityUpdatesButton.setEnabled(false);
        }
    }

    /**
     * Retrieves a SharedPreference object used to store or read values in this app. If a
     * preferences file passed as the first argument to {@link #getSharedPreferences}
     * does not exist, it is created when {@link SharedPreferences.Editor} is used to commit
     * data.
     */
    private SharedPreferences getSharedPreferencesInstance() {
        return getSharedPreferences(Csensors.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
    }

    /**
     * Retrieves the boolean from SharedPreferences that tracks whether we are requesting activity
     * updates.
     */
    private boolean getUpdatesRequestedState() {
        return getSharedPreferencesInstance()
                .getBoolean(Csensors.ACTIVITY_UPDATES_REQUESTED_KEY, false);
    }

    /**
     * Sets the boolean in SharedPreferences that tracks whether we are requesting activity
     * updates.
     */
    private void setUpdatesRequestedState(boolean requestingUpdates) {
        getSharedPreferencesInstance()
                .edit()
                .putBoolean(Csensors.ACTIVITY_UPDATES_REQUESTED_KEY, requestingUpdates)
                .apply();
    }

    /**
     * Stores the list of detected activities in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putSerializable(Csensors.DETECTED_ACTIVITIES, mDetectedActivities);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Processes the list of freshly detected activities. Asks the adapter to update its list of
     * DetectedActivities with new {@code DetectedActivity} objects reflecting the latest detected
     * activities.
     */
    protected void updateDetectedActivitiesList(ArrayList<DetectedActivity> detectedActivities) {
//        mAdapter.updateActivities(detectedActivities);
    }

    /**
     * Receiver for intents sent by DetectedActivitiesIntentService via a sendBroadcast().
     * Receives a list of one or more DetectedActivity objects associated with the current state of
     * the device.
     */
    int counter2 = 0;

    public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver {
        protected static final String TAG = "activity-detection-response-receiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            counter2++;
            Log.d(Constants.TAG, "ActivityDetection counter = " + counter2);
            mDetectedActivities =
                    intent.getParcelableArrayListExtra(Csensors.ACTIVITY_EXTRA);
            parseDetectedActivity(mDetectedActivities);
        }

    }

    public void parseDetectedActivity(ArrayList<DetectedActivity> mmDetectedActivities) {
        int ggg = mmDetectedActivities.size();
        int type = 0;
        int still_confidence = 0;
        int vehicle_confidence = 0;

        for (int i = 0; i < ggg; i++) {
            DetectedActivity da = mmDetectedActivities.get(i);
            type = da.getType();
            if (type == DetectedActivity.STILL) {
                still_confidence = da.getConfidence();
                Log.d(Constants.TAG, "still_confidence = " + still_confidence);
            } else if (type == DetectedActivity.IN_VEHICLE) {
                vehicle_confidence = da.getConfidence();
                Log.d(Constants.TAG, "vehicle_confidence = " + vehicle_confidence);
                if (still_confidence < Constants.STILL_CONFIDENCE_VALUE & vehicle_confidence > Constants.IN_VEHICLE_CONFIDENCE_VALUE) {
// Create the LocationRequest object
                    mLocationRequest = LocationRequest.create()
                            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                            .setInterval(Constants.LOCATION_REQUEST_UPDATES_INTERVAL);       // 5 seconds, in milliseconds
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                    Toasty.info(this, "requestLocationUpdates", Toast.LENGTH_SHORT).show();
                } else {
                    LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                    Toasty.normal(this, "removeLocationUpdates", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }
}