package com.krakenjaws.findfood.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.krakenjaws.findfood.R;

/**
 * Created by Andrew on 5/18/2019.
 */
public class MapActivity extends Fragment implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    MapView mMapView;
    private GoogleMap googleMap;
    private Location mCurrentLocation;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    public static MapActivity newInstance() {
        return new MapActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_map, container, false);

        mMapView = rootView.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;

//                googleMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
                // For showing a move to my location button
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                // enable location buttons
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);

                // fetch last location if any from provider - GPS.
//                final LocationManager locationManager = (LocationManager) getActivity().getSystemService(
//                        Context.LOCATION_SERVICE);
//                final Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//
//                if (loc == null) {
//
//                    final LocationListener locationListener = new LocationListener() {
//                        @Override
//                        public void onLocationChanged(final Location location) {
//
//                            // getting location of user
//                            final double latitude = location.getLatitude();
//                            final double longitude = location.getLongitude();
//                            // do something with Latlng
//                        }
//
//                        public void onStatusChanged(String provider, int status, Bundle extras) {
//                        }
//
//                        public void onProviderEnabled(String provider) {
//                            // do something
//                        }
//
//                        public void onProviderDisabled(String provider) {
//                            // notify user "GPS or Network provider" not available
//                        }
//                    };
//
//                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 500, (android.location.LocationListener) locationListener);
//                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 500, (android.location.LocationListener) locationListener);
//                }
//                else {
//                    // do something with last know location
//                }
//                // For dropping a marker at a point on the Map
//                LatLng sydney = new LatLng(-34, 151);
//                googleMap.addMarker(new MarkerOptions().position(sydney).title("Marker Title").snippet("Marker Description"));
//
//                // For zooming automatically to the location of the marker
//                CameraPosition cameraPosition = new CameraPosition.Builder().target(sydney).zoom(12).build();
//                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        });
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}