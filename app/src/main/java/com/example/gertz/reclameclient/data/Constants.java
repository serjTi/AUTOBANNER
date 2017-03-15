package com.example.gertz.reclameclient.data;

public class Constants {
    public static final String TAG = "TAG";
    public static final String UTF_8 = "UTF-8";
    public static final String MD5 = "MD5";
    public static final String EMAIL = "email";
    public static final String PASSWORD = "password";

    public static final String APP_PREFERENCES = "mysettings";
    public static final String TOKEN_PREFERENCES = "token_preferences";
    public static final String KEY_MEMORY_PLACE = "SAVED_MAMORY_PLACE";

    public static final String STATUS = "status";
    public static final String CAMPAIGN_ID = "campaign_id";
    public static final String SUCCESS = "success";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String DISTANCE = "distance";
    public static final String CURRENT_DISTANCE = "current_distance";
    public static final String ENCODED_PHOTO = "encoded_photo";
    public static final String AUTHENTICATION = "AUTHENTICATION";
    public static final String AUTHENTICATION_KEY = "842c90552ac1ee3f6c85cc3639686998";

    public static final String AUTHENTICATION_URL = "http://api.autobanner.biz/api/user/login";
    public static final String REPORT_URL = "http://api.autobanner.biz/api/user/report";
    public static final String LOGOUT_URL = "http://api.autobanner.biz/api/user/logout";

    public static final String LOCALITY = "check locality";
    public static final int YOU_CAN_MOVE = 500;
    public static final String LOCATION_DETECTED = "location";
    public static final int DISTANCE_UPDATED = 100;
    public static final int GPS_DISABLED = 600;
    public static final int CITY_LOCALITY = 700;
    public static final int OUT_OF_TOWN_LOCALITY = 701;
    public static final int CAMERA_REQUEST = 1888;
    public static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;


// parameters for activity detection and GPS
    public static int LOCATION_REQUEST_UPDATES_INTERVAL = 5 * 1000;
    public static int STILL_CONFIDENCE_VALUE = 75;
    public static int IN_VEHICLE_CONFIDENCE_VALUE = 33;
    public static float GPS_LOCATION_ACCURACY = 45.0f;


 // it's for tests
    public static final int NET_DIST_UPD = 584;
    public static final int BOTH_DIST_UPD = 84321;

// key for GoogleMapsApi
    private static final String API_KEY = "key=" + "AIzaSyCAnG0-7mO_eeSTE7Q1dLMZFVgvvYFkMto" + "_API";

}
