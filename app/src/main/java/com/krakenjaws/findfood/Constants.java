package com.krakenjaws.findfood;

/**
 * Created by Andrew on 5/8/2019.
 */
public class Constants {
    // This is where we want to put anything that we know is a constant
    // You should really use YOUR OWN API_KEY but it is ok to use mine if not shared
    public static final String GOOGLE_PLACES_API_KEY = "AIzaSyCgLleMvDsfxXUUMT78WSXaL1PkILcCWzI";
    // GCP API_KEY
    public static final String GCP_API_KEY = "AIzaSyBotPpDZ2bY5k05gul0jSmY5MLgRbyeQI8";
    // Strings
    public static final String BASE_URL = "https://maps.googleapis.com/maps/api/";
    public static final String PREFS_FILE_NAME = "sharedPreferences";
    public static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    // Integers
    public static final int TWO_MINUTES = 1000 * 60 * 2;
    public static final int ERROR_DIALOG_REQUEST = 9001;
    public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 9002;
    public static final int PERMISSIONS_REQUEST_ENABLE_GPS = 9003;
}
