package com.krakenjaws.findfood.ui;

import android.Manifest.permission;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.krakenjaws.findfood.APIClient;
import com.krakenjaws.findfood.R;
import com.krakenjaws.findfood.adapters.CardViewRecyclerViewAdapter;
import com.krakenjaws.findfood.modals.PlacesDetails_Modal;
import com.krakenjaws.findfood.models.UserLocation;
import com.krakenjaws.findfood.response.DistanceResponse;
import com.krakenjaws.findfood.response.PlacesResponse;
import com.krakenjaws.findfood.response.PlacesResponse.CustomA;
import com.krakenjaws.findfood.response.PlacesResponse.Root;
import com.krakenjaws.findfood.response.Places_details;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static com.krakenjaws.findfood.Constants.BASE_URL;
import static com.krakenjaws.findfood.Constants.ERROR_DIALOG_REQUEST;
import static com.krakenjaws.findfood.Constants.GCP_API_KEY;
import static com.krakenjaws.findfood.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.krakenjaws.findfood.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;
import static com.krakenjaws.findfood.Constants.PREFS_FILE_NAME;

/**
 * Our main view after logging in to our profile.
 */
public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    // Used for debugging
    private static final String TAG = "MainActivity";

    // Lists
    private ArrayList<PlacesResponse.CustomA> results;
    private ArrayList<PlacesDetails_Modal> details_modal;

    // Widgets
    private APIInterface mAPIService;
    private RelativeLayout mRelativeLayout;
    private RecyclerView mRestuarantsRecyclerView;
    private ProgressBar mProgressBar;

    // Variables
    private boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationClient;
    private GoogleApiClient mGoogleApiClient;
    private String mAddressOutput;
    private String mLatLngString;
    private Location mLastKnownLocation;
    private double mSourceLat;
    private double mSourceLng;
    // Radius specifies the radius of the circle around our gps lat,Lng coords in meters
    // TODO The recommended best practice is to set radius based on the accuracy of the location signal as given by the location sensor.
    //       Note that setting a radius biases results to the indicated area, but may not fully restrict results to the specified area.
    private final long radius = 3 * 1000;
    private FirebaseFirestore mDb;
    private UserLocation mUserLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: created");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Try to keep this clean with methods
        mRelativeLayout = findViewById(R.id.relLayout_main);
        mRestuarantsRecyclerView = findViewById(R.id.recyclerView_main);
        mProgressBar = findViewById(R.id.progressBar);

//        findViewById(R.id.fab_roll_random_dice).setOnClickListener(this);

        if (VERSION.SDK_INT >= VERSION_CODES.N) { // for newer apis
            mProgressBar.setProgress(0, true);
        } else { // older apis
            mProgressBar.setProgress(0);
        }
        showDialog();

        mAPIService = APIClient.getClient().create(APIInterface.class);

        mRestuarantsRecyclerView.setNestedScrollingEnabled(false);
        mRestuarantsRecyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRestuarantsRecyclerView.setLayoutManager(layoutManager);

        // To get our last known location
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mDb = FirebaseFirestore.getInstance();

        initSupportActionBar();

        // initalizing... Google Api Client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * This allows us to "find" the nearby restaurants
     *
     * @param placeType we want this to be changeable later.
     */
    private void fetchStores(String placeType) {
        Call<PlacesResponse.Root> call = mAPIService.doPlaces(mLatLngString, radius, placeType, GCP_API_KEY);
        call.enqueue(new Callback<Root>() {
            @Override
            public void onResponse(@NonNull Call<PlacesResponse.Root> call, @NonNull Response<PlacesResponse.Root> response) {
                PlacesResponse.Root root = response.body();

                if (response.isSuccessful()) {
                    switch (root.status) {
                        case "OK":
                            results = root.customA;
                            details_modal = new ArrayList<>();
                            String photoURL;
                            Log.i(TAG, "Fetching stores... Please Wait.");

                            for (int i = 0; i < results.size(); i++) {
                                CustomA info = results.get(i);
                                String place_id = results.get(i).place_id;

                                if (results.get(i).photos != null) {
                                    String photo_reference = results.get(i).photos.get(0).photo_reference;
                                    photoURL = BASE_URL + "place/photo?maxwidth=100&photoreference=" + photo_reference +
                                            "&key=" + GCP_API_KEY;
                                } else {
                                    photoURL = "N/A";
                                }
                                fetchDistance(info, place_id, photoURL);
                                Log.i("Coordinates  ", info.geometry.locationA.lat + "," + info.geometry.locationA.lng);
                                Log.i("Names  ", info.name);
                            }
                            break;
                        case "ZERO_RESULTS":
                            Toast.makeText(getApplicationContext(), "No matches found near you", Toast.LENGTH_SHORT).show();
                            hideDialog();
                            break;
                        case "OVER_QUERY_LIMIT":
                            Toast.makeText(getApplicationContext(), "You have reached the Daily Quota of Requests", Toast.LENGTH_SHORT).show();
                            hideDialog();
                            break;
                        default:
                            Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
                            hideDialog();
                            break;
                    }
                } else if (response.code() != 200) {
                    Toast.makeText(getApplicationContext(), "Error " + response.code() + " found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {
                Toast.makeText(getApplicationContext(), "Error in Fetching Details, Please Refresh", Toast.LENGTH_SHORT).show();
                call.cancel();
            }
        });
    }

    /**
     * This is how we determine how far away the restaurant is from our current location
     *
     * @param info     our latitude and longitude coordinates
     * @param place_id the unique id to our restaurant
     * @param photourl a link containing the image of the restaurant if it has one
     */
    private void fetchDistance(final PlacesResponse.CustomA info, final String place_id, final String photourl) {
        Log.i(TAG, "Distance API call started");
        Call<DistanceResponse> call = mAPIService.getDistance(mLatLngString, info.geometry.locationA.lat + "," + info.geometry.locationA.lng, GCP_API_KEY);
        call.enqueue(new Callback<DistanceResponse>() {
            @Override
            public void onResponse(@NonNull Call<DistanceResponse> call, @NonNull Response<DistanceResponse> response) {
                DistanceResponse resultDistance = response.body();
                Log.d(TAG, "resultDistance: " + resultDistance);

                if (response.isSuccessful()) {
                    Log.i(TAG, resultDistance.status);

                    if ("OK".equalsIgnoreCase(resultDistance.status)) {
                        DistanceResponse.InfoDistance row1 = resultDistance.rows.get(0);
                        DistanceResponse.InfoDistance.DistanceElement element1 = row1.elements.get(0);

                        if ("OK".equalsIgnoreCase(element1.status)) {
                            double distance; // I want the distance to be in miles! not km
                            DistanceResponse.InfoDistance.ValueItem itemDistance = element1.distance;
                            // convert meters to miles (1m = 0.0006213712mi)
                            distance = itemDistance.value * 0.0006213712;
                            // changed the format to look like '1.0 miles'
                            String total_distance = String.format(Locale.US, "%.1f miles", distance);
                            fetchPlace_details(info, place_id, total_distance, info.name, photourl);
                        }
                    }
                } else if (response.code() != 200) {
                    Toast.makeText(getApplicationContext(), "Error " + response.code() + " found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {
                Toast.makeText(getApplicationContext(), "Error in Fetching Details, Please Refresh", Toast.LENGTH_SHORT).show();
                call.cancel();
            }
        });

    }

    /**
     * The other imporant parts we want from the restaurant
     *
     * @param info          lat, lng
     * @param place_id      the restaurant #
     * @param totaldistance how far away in miles
     * @param name          the name of the restaurant
     * @param photourl      image of restaurant
     */
    private void fetchPlace_details(final PlacesResponse.CustomA info, final String place_id, final String totaldistance, final String name, final String photourl) {
        Call<Places_details> call = mAPIService.getPlaceDetails(place_id, GCP_API_KEY);
        call.enqueue(new Callback<Places_details>() {
            @Override
            public void onResponse(@NonNull Call<Places_details> call, @NonNull Response<Places_details> response) {
                Places_details details = response.body();

                if ("OK".equalsIgnoreCase(details.status)) {
                    float rating = details.result.rating;
                    String address = details.result.formatted_adress;
                    String phone = details.result.international_phone_number;

                    details_modal.add(new PlacesDetails_Modal(address, phone, rating, totaldistance, name, photourl));
                    Log.i("details: ", info.name + "  " + address);

                    if (details_modal.size() == results.size()) {
                        Collections.sort(details_modal, new Comparator<PlacesDetails_Modal>() {
                            @Override
                            public int compare(PlacesDetails_Modal lhs, PlacesDetails_Modal rhs) {
                                return lhs.distance.compareTo(rhs.distance);
                            }
                        });
                        CardViewRecyclerViewAdapter adapterStores = new CardViewRecyclerViewAdapter(getApplicationContext(), details_modal, mAddressOutput);
                        mRestuarantsRecyclerView.setAdapter(adapterStores);
                        adapterStores.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {
                call.cancel();
            }
        });
    }

    /**
     * Our current location formatted
     *
     * @param latLngString
     */
    private void fetchCurrentAddress(final String latLngString) {
        Call<Places_details> call = mAPIService.getCurrentAddress(latLngString, GCP_API_KEY);
        call.enqueue(new Callback<Places_details>() {
            @Override
            public void onResponse(@NonNull Call<Places_details> call, @NonNull Response<Places_details> response) {
                Places_details details = response.body();

                if ("OK".equalsIgnoreCase(details.status)) {
                    mAddressOutput = details.results.get(0).formatted_adress;
                    Log.i("Addr Current and coords", mAddressOutput + latLngString);
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {
                call.cancel();
            }
        });
    }

    private void getUserDetails() {
//        if (mUserLocation == null) {
//            mUserLocation = new UserLocation();
//
//            DocumentReference userRef = mDb.collection(getString(R.string.collection_users))
//                    .document(FirebaseAuth.getInstance().getUid());
//
//            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//                @Override
//                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                    if (task.isSuccessful()) {
//                        Log.d(TAG, "onComplete: successfully got the user details");
//                        User user = task.getResult().toObject(User.class);
//                        mUserLocation.setUser(user);
//                        getLastKnownLocation();
//                    }
//                }
//            });
//        }
    }

    private void saveUserLocation() {

        if (mUserLocation != null) {
            DocumentReference locationRef = mDb.collection(getString(R.string.collection_user_locations))
                    .document(FirebaseAuth.getInstance().getUid());

            locationRef.set(mUserLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "saveUserLocation: \ninserted user location into database. +" +
                                "\n latitude: " + mUserLocation.getGeo_point().getLatitude() +
                                "\n longitude: " + mUserLocation.getGeo_point().getLongitude());
                    }
                }
            });
        }
    }

    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation: called");
        if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful()) {
                    Location location = task.getResult();
                    GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    Log.d(TAG, "onComplete: latitude: " + geoPoint.getLatitude());
                    Log.d(TAG, "onComplete: longitude: " + geoPoint.getLongitude());

                    mUserLocation.setGeo_point(geoPoint);
                    mUserLocation.setTimestamp(null);
                    saveUserLocation();
                }
            }
        });
    }

    /**
     * Checking for Google Play Services and then GPS
     */
    private boolean checkMapServices() {
        if (isServicesOK()) {
            return isMapsEnabled();
        }
        return false;
    }

    /**
     * Dialog to show user to enable GPS
     */
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Does user have GPS enabled?
     */
    private boolean isMapsEnabled() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
//            getChatrooms();
//            getUserLocation();
//            getUserDetails();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Does user have Google Play Services enabled?
     */
    private boolean isServicesOK() {
        Log.d(TAG, "isServicesOK: checking Google Play Services version");
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if (available == ConnectionResult.SUCCESS) {
            //everything is fine and the user can make activity_map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occurred but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(
                    MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "You can't make activity_map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    /**
     * Location permission results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i("On request permiss", "executed");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
//                getUserLocation();
//                getUserDetails();
            } else {
                Toast.makeText(getApplicationContext(), "Please ALLOW GPS to access full features.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * When there is an answer to the permissions dialog
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: called");
        if (requestCode == PERMISSIONS_REQUEST_ENABLE_GPS) {
            if (mLocationPermissionGranted) {
//                    getChatrooms();
                getUserLocation();
                getUserDetails();
            } else {
                getLocationPermission();
            }
        }
    }

    private void initSupportActionBar() {
        setTitle("Nearby Restaurants");
    }

    /**
     * The crazy amount of permissions we need to get our location
     */
    private void getUserLocation() {

        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (VERSION.SDK_INT >= VERSION_CODES.M) { // for apis at 23 or above

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        ACCESS_FINE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(
                        this, ACCESS_COARSE_LOCATION)) {
                    Log.d(TAG, "getUserLocation: we are showing the alert for this");
                    showAlert();
                } else {

                    if (isFirstTimeAskingPermission(this, permission.ACCESS_FINE_LOCATION)) {
                        Log.d(TAG, "getUserLocation: asking for 1st time permission");
                        firstTimeAskingPermission(this, permission.ACCESS_FINE_LOCATION, false);
                        ActivityCompat.requestPermissions(this, new String[]
                                        {
                                                ACCESS_COARSE_LOCATION,
                                                ACCESS_FINE_LOCATION
                                        },
                                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                    } else {
                        Log.d(TAG, "getUserLocation: you must select YES");
                        Toast.makeText(this, "You will not be able to access the" +
                                " full features of this app", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                mLocationPermissionGranted = true;
                getUserDetails();
            }
        } else {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(this,
                    new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                mLastKnownLocation = location;
                                mSourceLat = location.getLatitude();
                                mSourceLng = location.getLongitude();
                                GeoPoint geoPoint = new GeoPoint(mSourceLat, mSourceLng);
                                mLatLngString = mSourceLat + "," + mSourceLng;
                                fetchCurrentAddress(mLatLngString);
                                fetchStores("restaurant");
                                hideDialog();
                                Log.i(TAG, "onSuccess: LatLng = " + mLatLngString);
                            } else { // TODO: get location update if (location == null);
                                Log.d(TAG, "Failed to find last known location");
                                Toast.makeText(getApplicationContext(), "Error in fetching the" +
                                        " last known location, try refreshing", Toast.LENGTH_SHORT).show();
                                getUserDetails();
                            }
                        }
                    });
        }
    }

    /**
     * Asking the User to turn on GPS with a popup [ DIALOG ]
     */
    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Location Settings are OFF \nPlease Enable Location")
                .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{
                                        ACCESS_COARSE_LOCATION,
                                        ACCESS_FINE_LOCATION
                                },
                                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        // ...
                    }
                });
        dialog.show();
    }

    private static void firstTimeAskingPermission(Context context, String permission, boolean isFirstTime) {
        Log.d(TAG, "firstTimeAskingPermission: true");
        SharedPreferences sharedPreference = context.getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE);
        sharedPreference.edit().putBoolean(permission, isFirstTime).apply();
    }

    private static boolean isFirstTimeAskingPermission(Context context, String permission) {
        Log.d(TAG, "isFirstTimeAskingPermission: true");
        return context.getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE).getBoolean(permission, true);
    }

    /**
     * This is basically unavoidable so its good to check for Google Map Services/GPS here
     */
    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: resume");
        super.onResume();
        if (checkMapServices()) {
            if (mLocationPermissionGranted) {
                mGoogleApiClient.reconnect();
                //        getChatrooms();
                getUserLocation();
                getUserDetails();
//                setUpMapIfNeeded();
//                getLastKnownLocation();
                // getLocationUpdate
            } else {
                getLocationPermission();
            }
        }
    }


    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: true");
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: true");
        super.onPause();
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: started");
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        } else {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    /**
     * When we want to leave the app :'(
     */
    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // TODO: debug this
    private void inflateUserListFragment() {
        hideSoftKeyboard();

        MapActivity fragment = MapActivity.newInstance();
//        Bundle bundle = new Bundle();
//        bundle.putParcelableArrayList(getString(R.string.intent_user_list), mUserList);
//        fragment.setArguments(bundle);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up);
        transaction.replace(R.id.main_container, fragment, getString(R.string.fragment_map));
        transaction.addToBackStack(getString(R.string.fragment_map));
        transaction.commit();
    }

    private void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh: {
                showDialog();
                item.setVisible(false);
                inflateUserListFragment(); // Sweet this works now!
                return true;
            }
            // We can make this a menu item or an icon action like the refresh button
            case R.id.action_chatroom: {
                startActivity(new Intent(this, ChatroomActivity.class));
                return true;
            }
            case R.id.action_profile: {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            case R.id.action_sign_out: {
                signOut();
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }

    }

    private void showDialog() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideDialog() {
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected: connected");
        if (mLocationPermissionGranted) {
            mGoogleApiClient.connect();
            getUserLocation();
            getUserDetails();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended: suspended");
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: Error: " + connectionResult.getErrorCode());
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onClick(View v) {
        // TODO: add a floating dice button that randomly chooses a cardview restaurant from the list
    }
}