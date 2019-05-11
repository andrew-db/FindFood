package com.krakenjaws.findfood.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import com.google.firebase.auth.FirebaseAuth;
import com.krakenjaws.findfood.R;

import static com.krakenjaws.findfood.Constants.ERROR_DIALOG_REQUEST;
import static com.krakenjaws.findfood.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.krakenjaws.findfood.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    //widgets
    private RelativeLayout mRelativeLayout;
    private ProgressBar mProgressBar;

    //vars
//    private ArrayList<Chatroom> mChatrooms = new ArrayList<>();
//    private Set<String> mChatroomIds = new HashSet<>();
//    private ChatroomRecyclerAdapter mChatroomRecyclerAdapter;
    private RecyclerView mRestuarantsRecyclerView;
    //    private ListenerRegistration mChatroomEventListener;
//    private FirebaseFirestore mDb;
    private boolean mLocationPermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Try to keep this clean with methods
        mRelativeLayout = findViewById(R.id.relLayout_main);
        mRestuarantsRecyclerView = findViewById(R.id.recyclerView_main);
        mProgressBar = findViewById(R.id.progressBar);
//        findViewById(R.id.fab_create_chatroom).setOnClickListener(this);

//        mDb = FirebaseFirestore.getInstance();

        initSupportActionBar();
//        initChatroomRecyclerView();
        checkForInternet();
    }


    private boolean checkMapServices() {
        if (isServicesOK()) {
            if (isMapsEnabled()) {
                return true;
            }
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
     *
     * @return
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
            // getUserLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Does user have Google Play Services enabled?
     *
     * @return
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
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: called.");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ENABLE_GPS: {
                if (mLocationPermissionGranted) {
//                    getChatrooms();
                    //            getUserLocation();
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
            message = "Good! Connected to Internet";
            color = Color.WHITE;
//            getUserLocation(); // debug this
        } else {
            message = "Sorry! Not connected to internet";
            color = Color.RED;
        }

        Snackbar snackbar = Snackbar
                .make(mRelativeLayout, message, Snackbar.LENGTH_LONG);

        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(color);
        snackbar.show();
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
        super.onResume();
        if (checkMapServices()) {
            if (mLocationPermissionGranted) {
                //        getChatrooms();
                // getLocationUser();
            } else {
                getLocationPermission();
            }
        }
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
}