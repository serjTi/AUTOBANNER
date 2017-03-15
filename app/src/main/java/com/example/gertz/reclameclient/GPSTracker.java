package com.example.gertz.reclameclient;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

import com.example.gertz.reclameclient.data.Constants;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class GPSTracker extends Service implements LocationListener {

    public static final String TAG = "TAG";

    public GPSTracker() {
        super();
        // TODO Auto-generated constructor stub
    }

    private final IBinder mIBinder = new LocalBinder();
    private Handler mHandler = null;
    // flag for GPS status
    boolean isGPSEnabled = false;
    // flag for network status
    boolean isNetworkEnabled = false;
    boolean canGetLocation = false;

    Location location = null; // location

    float GPSdistance = 0; // in meters
    int counter = 0;


    Location startGPSLocation = null; // location
    Location startNETLocation = null; // location
    Location finishLocation = null; // location
    Location locationGPS_A = null; // location
    Location locationGPS_B = null; // location

    double latitude; // latitude
    double longitude; // longitude

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 200; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 10 * 1; // 10 sec

    // Declaring counter Location Manager
    protected LocationManager locationManager;

    @Override
    public IBinder onBind(Intent intent) {
        return mIBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("TAG", "GPSservice onCreate()");
        initializeGPS();
    }

    private void initializeGPS() {
        try {
            locationManager = (LocationManager) this
                    .getSystemService(Context.LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);
            Log.d(Constants.TAG, "isGPSEnabled = " + isGPSEnabled);
            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            Log.d(Constants.TAG, "isNetworkEnabled = " + isNetworkEnabled);

            canGetLocation = canGetLocation();
            Log.d(Constants.TAG, "canGetLocation = " + canGetLocation);
            if (canGetLocation) {
                if (locationManager != null) {
                    startNETLocation = locationManager
                            .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    startGPSLocation = locationGPS_A = locationManager
                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
            } else {
                showSettingsAlert();
                Log.d(TAG, "Navigation Disabled");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int startId) {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        stopUsingGPS();
        if (mHandler != null) {
            mHandler = null;
        }
    }

    public class LocalBinder extends Binder {
        public GPSTracker getInstance() {
            return GPSTracker.this;
        }
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }


    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     */
    public void stopUsingGPS() {
        if (locationManager != null) {
            locationManager.removeUpdates(GPSTracker.this);
        }
    }

    public double getLatitude() {
        if (location != null) {
            latitude = location.getLatitude();
        }
        // return latitude
        return latitude;
    }

    public double getLongitude() {
        if (location != null) {
            longitude = location.getLongitude();
        }
        return longitude;
    }

    public boolean canGetLocation() {
        if (isGPSEnabled && isNetworkEnabled) {
            return true;
        } else
            return false;
    }

    public void showSettingsAlert() {
        final Context c = this;
        AlertDialog alertDialog = new AlertDialog.Builder(c)
                .setTitle("Ошибка")
                .setMessage("Навигация выключена")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setNeutralButton("Настройки", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                c.startActivity(intent);
                    }
                })
                .create();
        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alertDialog.show();


//        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
//
//        // Setting Dialog Title
//        alertDialog.setTitle("GPS is settings");
//
//        // Setting Dialog Message
//        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");
//
//
//        // On pressing Settings button
//        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                startActivity(intent);
//            }
//        });
//
//        // on pressing cancel button
//        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.cancel();
//            }
//        });
//        alertDialog.create();
//        // Showing Alert Message
//        alertDialog.show();
    }

    private void toDistance(Location locationA, Location locationB) {

        if (locationA != null & locationB != null) {
            GPSdistance += locationA.distanceTo(locationB);
            if (mHandler != null) {
                Message mDistanceUpdated = new Message();
                mDistanceUpdated.what = Constants.DISTANCE_UPDATED;
                Bundle data = new Bundle();
                data.putFloat(Constants.DISTANCE, GPSdistance);
                mDistanceUpdated.setData(data);
                mHandler.sendMessage(mDistanceUpdated);
                Log.d(TAG, "toDistance");
                Log.d(TAG, mDistanceUpdated.toString());
            }

        }

    }

    @Override
    public void onLocationChanged(Location location) {
        checkLocality(location);
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            counter++;
            Log.d(TAG, "location changed" + String.valueOf(counter));
            if (counter == 1) {
                locationGPS_B = location;
            } else if (counter > 1) {
                locationGPS_A = locationGPS_B;
                locationGPS_B = location;
                toDistance(locationGPS_A, locationGPS_B);
                finishLocation = location;
            }
        }
    }

    private void checkLocality(Location location) {
        Geocoder gcd = new Geocoder(getApplicationContext(), Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = gcd.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses.size() > 0) {
                String stringlocality = addresses.get(0).getLocality();
                if (stringlocality != null) {
                    Log.d("TAG", "locality = " + stringlocality.toString());
                    if (stringlocality.equals("Одесса")) {
                        Log.d("TAG", "locality = Odessa");

                        // TODO send locality message
                        if (mHandler != null) {
                            Message localityMessage = new Message();
                            localityMessage.what = Constants.CITY_LOCALITY;
                            Bundle data = new Bundle();
                            data.putString(Constants.LOCALITY, stringlocality);
                            localityMessage.setData(data);
                            mHandler.sendMessage(localityMessage);
                            Log.d(TAG, "locality was send");
                            Log.d(TAG, localityMessage.toString());
                        }


                    } else {
                        Log.d("TAG", "Вы выехали за пределы города");
                        if (mHandler != null) {
                            Message localityMessage = new Message();
                            localityMessage.what = Constants.OUT_OF_TOWN_LOCALITY;
                            Bundle data = new Bundle();
                            data.putString(Constants.LOCALITY, "Вы выехали за пределы города");
                            localityMessage.setData(data);
                            mHandler.sendMessage(localityMessage);
                            Log.d(TAG, "locality was send");
                            Log.d(TAG, localityMessage.toString());
                        }
                    }
                    Log.d("TAG", "addresses.get(0).getLocality() = " + addresses.get(0).getLocality());
                } else if (stringlocality == null) {
                    Log.d("TAG", "locality == null Вы выехали за пределы города. Бабла не получите!");
                    if (mHandler != null) {
                        Message localityMessage = new Message();
                        localityMessage.what = Constants.OUT_OF_TOWN_LOCALITY;
                        Bundle data = new Bundle();
                        data.putString(Constants.LOCALITY, "Вы выехали за пределы города");
                        localityMessage.setData(data);
                        mHandler.sendMessage(localityMessage);
                        Log.d(TAG, "locality was send");
                        Log.d(TAG, localityMessage.toString());
                    }
                }
            } else {
                // do your staff
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("TAG", "adresses troubles = " + e.toString());
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


}