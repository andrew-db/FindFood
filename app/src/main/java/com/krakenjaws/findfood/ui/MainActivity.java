package com.krakenjaws.findfood.ui;

import android.Manifest.permission;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.krakenjaws.findfood.APIClient;
import com.krakenjaws.findfood.R;
import com.krakenjaws.findfood.adapters.Rv_adapter;
import com.krakenjaws.findfood.modals.PlacesDetails_Modal;
import com.krakenjaws.findfood.response.DistanceResponse;
import com.krakenjaws.findfood.response.PlacesResponse;
import com.krakenjaws.findfood.response.PlacesResponse.CustomA;
import com.krakenjaws.findfood.response.PlacesResponse.Root;
import com.krakenjaws.findfood.response.Places_details;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static com.krakenjaws.findfood.Constants.BASE_URL;
import static com.krakenjaws.findfood.Constants.ERROR_DIALOG_REQUEST;
import static com.krakenjaws.findfood.Constants.GCP_API_KEY;
import static com.krakenjaws.findfood.Constants.MY_PERMISION_CODE;
import static com.krakenjaws.findfood.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.krakenjaws.findfood.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;
import static com.krakenjaws.findfood.Constants.PREFS_FILE_NAME;


public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "MainActivity";

    //lists
    ArrayList<PlacesResponse.CustomA> results;
    ArrayList<PlacesDetails_Modal> details_modal;


    //widgets
    private APIInterface mAPIService;
    private RelativeLayout mRelativeLayout;
    private RecyclerView mRestuarantsRecyclerView;
    private ProgressBar mProgressBar;
//    private TextView mTextView;

    //vars
    private boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private GoogleApiClient mGoogleApiClient;
    private String mAddressOutput;
    private String mLatLngString;
    protected Location mLastLocation;
    public double mSourceLat, mSourceLng;
    // Radius specifies the radius of the circle around our gps latLng coord in meters
    private long radius = 11 * 1000; // 10miles range
    public String Location_type = "ROOFTOP";

//    private ArrayList<Chatroom> mChatrooms = new ArrayList<>();
//    private Set<String> mChatroomIds = new HashSet<>();
//    private ChatroomRecyclerAdapter mChatroomRecyclerAdapter;
    //    private ListenerRegistration mChatroomEventListener;
//    private FirebaseFirestore mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Try to keep this clean with methods
        mRelativeLayout = findViewById(R.id.relLayout_main);
        mRestuarantsRecyclerView = findViewById(R.id.recyclerView_main);
        mProgressBar = findViewById(R.id.progressBar);
//        mTextView = findViewById(R.id.location_tv);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mProgressBar.setProgress(0, true);
        } else {
            mProgressBar.setProgress(0);
        }
        mAPIService = APIClient.getClient().create(APIInterface.class);

        mRestuarantsRecyclerView.setNestedScrollingEnabled(false);
        mRestuarantsRecyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRestuarantsRecyclerView.setLayoutManager(layoutManager);

        // To get our location
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
//        findViewById(R.id.fab_create_chatroom).setOnClickListener(this);

//        mDb = FirebaseFirestore.getInstance();

        initSupportActionBar();
//        initChatroomRecyclerView();
        checkForInternet();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }


    private void fetchStores(String placeType) {

        Call<PlacesResponse.Root> call = mAPIService.doPlaces(mLatLngString, radius, placeType, GCP_API_KEY);
        call.enqueue(new Callback<Root>() {
            @Override
            public void onResponse(Call<PlacesResponse.Root> call, Response<PlacesResponse.Root> response) {
                PlacesResponse.Root root = (Root) response.body();


                if (response.isSuccessful()) {

                    switch (root.status) {
                        case "OK":

                            results = root.customA;

                            details_modal = new ArrayList<PlacesDetails_Modal>();
                            String photourl;
                            Log.i(TAG, "fetch stores");


                            for (int i = 0; i < results.size(); i++) {

                                CustomA info = (CustomA) results.get(i);

                                String place_id = results.get(i).place_id;


                                if (results.get(i).photos != null) {

                                    String photo_reference = results.get(i).photos.get(0).photo_reference;

                                    photourl = BASE_URL + "place/photo?maxwidth=100&photoreference=" + photo_reference +
                                            "&key=" + GCP_API_KEY;

                                } else {
                                    photourl = "NA";
                                }

                                fetchDistance(info, place_id, photourl);


                                Log.i("Coordinates  ", info.geometry.locationA.lat + " , " + info.geometry.locationA.lng);
                                Log.i("Names  ", info.name);

                            }

                            break;
                        case "ZERO_RESULTS":
                            Toast.makeText(getApplicationContext(), "No matches found near you", Toast.LENGTH_SHORT).show();
                            mProgressBar.setVisibility(View.GONE);
                            break;
                        case "OVER_QUERY_LIMIT":
                            Toast.makeText(getApplicationContext(), "You have reached the Daily Quota of Requests", Toast.LENGTH_SHORT).show();
                            mProgressBar.setVisibility(View.GONE);
                            break;
                        default:
                            Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
                            mProgressBar.setVisibility(View.GONE);
                            break;
                    }

                } else if (response.code() != 200) {
                    Toast.makeText(getApplicationContext(), "Error " + response.code() + " found.", Toast.LENGTH_SHORT).show();
                }


            }

            @Override
            public void onFailure(Call call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Error in Fetching Details,Please Refresh", Toast.LENGTH_SHORT).show();
                call.cancel();
            }
        });

    }


    private void fetchDistance(final PlacesResponse.CustomA info, final String place_id, final String photourl) {

        Log.i(TAG, "Distance API call start");

        Call<DistanceResponse> call = mAPIService.getDistance(mLatLngString, info.geometry.locationA.lat + "," + info.geometry.locationA.lng, GCP_API_KEY);

        call.enqueue(new Callback<DistanceResponse>() {
            @Override
            public void onResponse(Call<DistanceResponse> call, Response<DistanceResponse> response) {

                DistanceResponse resultDistance = (DistanceResponse) response.body();
                Log.d(TAG, "resultDistance: " + resultDistance);

                if (response.isSuccessful()) {

                    Log.i(TAG, resultDistance.status);

                    if ("OK".equalsIgnoreCase(resultDistance.status)) {
                        DistanceResponse.InfoDistance row1 = resultDistance.rows.get(0);
                        DistanceResponse.InfoDistance.DistanceElement element1 = row1.elements.get(0);

                        if ("OK".equalsIgnoreCase(element1.status)) {

                            DistanceResponse.InfoDistance.ValueItem itemDistance = element1.distance;

                            double dist; // I want the distance to be in miles! not km so this is done here
                            // convert meters to mi (1m = 0.0006213712mi)
                            dist = itemDistance.value * 0.0006213712;

                            String total_distance = dist + " miles";

                            fetchPlace_details(info, place_id, total_distance, info.name, photourl);
                        }


                    }

                } else if (response.code() != 200) {
                    Toast.makeText(getApplicationContext(), "Error " + response.code() + " found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Error in Fetching Details,Please Refresh", Toast.LENGTH_SHORT).show();
                call.cancel();
            }
        });

    }


    private void fetchPlace_details(final PlacesResponse.CustomA info, final String place_id, final String totaldistance, final String name, final String photourl) {

        Call<Places_details> call = mAPIService.getPlaceDetails(place_id, GCP_API_KEY);
        call.enqueue(new Callback<Places_details>() {
            @Override
            public void onResponse(Call<Places_details> call, Response<Places_details> response) {

                Places_details details = (Places_details) response.body();

                if ("OK".equalsIgnoreCase(details.status)) {

                    String address = details.result.formatted_adress;
                    String phone = details.result.international_phone_number;
                    float rating = details.result.rating;

                    details_modal.add(new PlacesDetails_Modal(address, phone, rating, totaldistance, name, photourl));

                    Log.i("details : ", info.name + "  " + address);

                    if (details_modal.size() == results.size()) {

                        Collections.sort(details_modal, new Comparator<PlacesDetails_Modal>() {
                            @Override
                            public int compare(PlacesDetails_Modal lhs, PlacesDetails_Modal rhs) {
                                return lhs.distance.compareTo(rhs.distance);
                            }
                        });

                        mProgressBar.setVisibility(View.GONE);
                        Rv_adapter adapterStores = new Rv_adapter(getApplicationContext(), details_modal, mAddressOutput);
                        mRestuarantsRecyclerView.setAdapter(adapterStores);
                        adapterStores.notifyDataSetChanged();
                    }

                }

            }

            @Override
            public void onFailure(Call call, Throwable t) {
                call.cancel();
            }
        });

    }


    private void fetchCurrentAddress(final String latLngString) {

        Call<Places_details> call = mAPIService.getCurrentAddress(latLngString,
                GCP_API_KEY);
        call.enqueue(new Callback<Places_details>() {
            @Override
            public void onResponse(Call<Places_details> call, Response<Places_details> response) {

                Places_details details = response.body();

                if ("OK".equalsIgnoreCase(details.status)) {
                    mAddressOutput = details.results.get(0).formatted_adress;
                    Log.i("Addr Current and coord.", mAddressOutput + latLngString);
                }

            }

            @Override
            public void onFailure(Call call, Throwable t) {
                call.cancel();
            }
        });

    }

    private boolean checkMapServices() {
        if (isServicesOK()) {
            return isMapsEnabled();
        }
        return false;
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(
                                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Does user have GPS enabled?
     */
    public boolean isMapsEnabled() {
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
            getUserLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Does user have Google Play Services enabled?
     */
    public boolean isServicesOK() {
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                MainActivity.this);

        if (available == ConnectionResult.SUCCESS) {
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(
                    MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT)
                    .show();
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i("On request permiss", "executed");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {

            case MY_PERMISION_CODE:

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    getUserLocation();
                } else {
                    showAlert();
                    mLocationPermissionGranted = false;
                    Toast.makeText(getApplicationContext(), "Please switch on GPS to access the features", Toast.LENGTH_LONG).show();
                    mProgressBar.setVisibility(View.GONE);

                }
                break;

        }
    }
//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        mLocationPermissionGranted = false;
//        switch (requestCode) {
//            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    mLocationPermissionGranted = true;
//                }
//            }
//        }
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: called.");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ENABLE_GPS: {
                if (mLocationPermissionGranted) {
//                    getChatrooms();
                    getUserLocation();
                    if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(this,
                            new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    if (location != null) {
                                        mLastLocation = location;
                                        mSourceLat = location.getLatitude();
                                        mSourceLng = location.getLongitude();
                                        mLatLngString = location.getLatitude() + ", " + location.getLongitude();

                                        fetchCurrentAddress(mLatLngString);
                                        Log.d(TAG, "onSuccess: LatLngString = " + mLatLngString);

                                        fetchStores("restaurant");
//                                    mTextView.setText(mLatLngString); // lets see if this changes the TextView
                                    } else {
                                        Log.d(TAG, "onSuccess: failed to find location");
                                        mProgressBar.setVisibility(View.GONE);
                                        Toast.makeText(getApplicationContext(), "Error in fetching the" +
                                                " location", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                } else {
                    getLocationPermission();
                }
            }
        }
    }

    /**
     * Shows us a cool snackbar if we have internet or not
     */
    private void checkForInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            showSnack(true);
        } else {
            mProgressBar.setVisibility(View.GONE);
            showSnack(false);
        }
    }

    private void initSupportActionBar() {
        setTitle("Find Nearby Restaurants");
    }

    /**
     * True if we are connected or False if we are not
     *
     * @param isConnected
     */
    public void showSnack(boolean isConnected) {
        int color;
        String message;

        if (isConnected) {
            message = "Sweet! Connected to the internet";
            color = Color.WHITE;
            getUserLocation(); // debug this
        } else {
            message = "Sorry! Not connected to the internet";
            color = Color.RED;
        }

        Snackbar snackbar = Snackbar
                .make(mRelativeLayout, message, Snackbar.LENGTH_LONG);

        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(color);
        snackbar.show();
    }

    private void getUserLocation() {

        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        ACCESS_FINE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(
                        this, ACCESS_COARSE_LOCATION)) {
                    showAlert();
                    Log.d(TAG, "getUserLocation: we showing the alert for this");
                } else {
                    if (isFirstTimeAskingPermission(this, permission.ACCESS_FINE_LOCATION)) {
                        Log.d(TAG, "getUserLocation: asking");
                        firstTimeAskingPermission(this,
                                permission.ACCESS_FINE_LOCATION, false);
                        ActivityCompat.requestPermissions(this,
                                new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION},
                                MY_PERMISION_CODE);
                    } else {
                        Log.d(TAG, "getUserLocation: you must say YES");
                        Toast.makeText(this, "You won't be able to access the" +
                                " features of this App", Toast.LENGTH_LONG).show();
                        mProgressBar.setVisibility(View.GONE);
                        //Permission disable by device policy or user denied permanently.
                        // Show proper error message
                    }

                }
            } else {
                mLocationPermissionGranted = true;
            }
        } else {
            mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(this,
                    new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {

                            if (location != null) {
                                mLastLocation = location;
                                mSourceLat = location.getLatitude();
                                mSourceLng = location.getLongitude();
                                mLatLngString = location.getLatitude() + ", " + location.getLongitude();

                                fetchCurrentAddress(mLatLngString);
                                Log.d(TAG, "onSuccess: LatLngString = " + mLatLngString);

                                fetchStores("restaurant");

                            } else {
                                Log.d(TAG, "onSuccess: failed to find location");
                                mProgressBar.setVisibility(View.GONE);
                                Toast.makeText(getApplicationContext(), "Error in fetching the" +
                                        " location", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings are OFF \nPlease Enable Location")
                .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {


                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION},
                                MY_PERMISION_CODE);


                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                    }
                });
        dialog.show();
    }

    public static void firstTimeAskingPermission(Context context, String permission,
                                                 boolean isFirstTime) {
        Log.d(TAG, "firstTimeAskingPermission: true");
        SharedPreferences sharedPreference = context.getSharedPreferences(PREFS_FILE_NAME,
                MODE_PRIVATE);
        sharedPreference.edit().putBoolean(permission, isFirstTime).apply();
    }

    public static boolean isFirstTimeAskingPermission(Context context, String permission) {
        Log.d(TAG, "isFirstTimeAskingPermission: true");
        return context.getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE).getBoolean(permission,
                true);
    }

    @Override
    public void onClick(View view) {
//        if (view.getId() == R.id.fab_create_chatroom) {
//            newChatroomDialog();
//        }
    }

//    private void initChatroomRecyclerView() {
//        mChatroomRecyclerAdapter = new ChatroomRecyclerAdapter(mChatrooms, this);
//        mChatroomRecyclerView.setAdapter(mChatroomRecyclerAdapter);
//        mChatroomRecyclerView.setLayoutManager(new LinearLayoutManager(this));
//    }

//    private void getChatrooms() {
//
//        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
//                .setTimestampsInSnapshotsEnabled(true)
//                .build();
//        mDb.setFirestoreSettings(settings);
//
//        CollectionReference chatroomsCollection = mDb
//                .collection(getString(R.string.collection_chatrooms));
//
//        mChatroomEventListener = chatroomsCollection.addSnapshotListener(
//        new EventListener<QuerySnapshot>() {
//            @Override
//            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
//            @Nullable FirebaseFirestoreException e) {
//                Log.d(TAG, "onEvent: called.");
//
//                if (e != null) {
//                    Log.e(TAG, "onEvent: Listen failed.", e);
//                    return;
//                }
//
//                if (queryDocumentSnapshots != null) {
//                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
//
//                        Chatroom chatroom = doc.toObject(Chatroom.class);
//                        if (!mChatroomIds.contains(chatroom.getChatroom_id())) {
//                            mChatroomIds.add(chatroom.getChatroom_id());
//                            mChatrooms.add(chatroom);
//                        }
//                    }
//                    Log.d(TAG, "onEvent: number of chatrooms: " + mChatrooms.size());
//                    mChatroomRecyclerAdapter.notifyDataSetChanged();
//                }
//
//            }
//        });
//    }

//    private void buildNewChatroom(String chatroomName) {
//
//        final Chatroom chatroom = new Chatroom();
//        chatroom.setTitle(chatroomName);
//
//        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
//                .setTimestampsInSnapshotsEnabled(true)
//                .build();
//        mDb.setFirestoreSettings(settings);
//
//        DocumentReference newChatroomRef = mDb
//                .collection(getString(R.string.collection_chatrooms))
//                .document();
//
//        chatroom.setChatroom_id(newChatroomRef.getId());
//
//        newChatroomRef.set(chatroom).addOnCompleteListener(new OnCompleteListener<Void>() {
//            @Override
//            public void onComplete(@NonNull Task<Void> task) {
//                hideDialog();
//
//                if (task.isSuccessful()) {
//                    navChatroomActivity(chatroom);
//                } else {
//                    View parentLayout = findViewById(android.R.id.content);
//                    Snackbar.make(parentLayout, "Something went wrong.", Snackbar.LENGTH_SHORT)
//                    .show();
//                }
//            }
//        });
//    }

//    private void navChatroomActivity(Chatroom chatroom) {
//        Intent intent = new Intent(MainActivity.this, ChatroomActivity.class);
//        intent.putExtra(getString(R.string.intent_chatroom), chatroom);
//        startActivity(intent);
//    }

//    private void newChatroomDialog() {
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Enter a chatroom name");
//
//        final EditText input = new EditText(this);
//        input.setInputType(InputType.TYPE_CLASS_TEXT);
//        builder.setView(input);
//
//        builder.setPositiveButton("CREATE", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                if (!input.getText().toString().equals("")) {
//                    buildNewChatroom(input.getText().toString());
//                } else {
//                    Toast.makeText(MainActivity.this, "Enter a chatroom name", Toast.LENGTH_SHORT)
//                    .show();
//                }
//            }
//        });
//        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.cancel();
//            }
//        });
//
//        builder.show();
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (mChatroomEventListener != null) {
//            mChatroomEventListener.remove();
//        }
    }

    /**
     * This is basically unavoidable so good to check GPS here
     */
    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: true");
        super.onResume();
        if (checkMapServices()) {
            if (mLocationPermissionGranted) {
                //        getChatrooms();
                mGoogleApiClient.isConnected();
                // getLocationUser();
            } else {
                getLocationPermission();
            }
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: true");
        super.onPause();
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: true");
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    //    @Override
//    public void onChatroomSelected(int position) {
//        navChatroomActivity(mChatrooms.get(position));
//    }

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


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh: {
                mProgressBar.setVisibility(View.VISIBLE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mProgressBar.setProgress(0, true);
                } else {
                    mProgressBar.setProgress(0);
                }

                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    showSnack(true);
                } else {
                    mProgressBar.setVisibility(View.GONE);
                    showSnack(false);
                }
                return true;
            }
            case R.id.action_sign_out: {
                signOut();
                return true;
            }
            case R.id.action_profile: {
                startActivity(new Intent(this, ProfileActivity.class));
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
        Log.d(TAG, "onConnected: Yes!");
        if (mLocationPermissionGranted) {
            getUserLocation();
        }
//          requestLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended: yep :/");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: Error: " + connectionResult.getErrorCode());
    }
}