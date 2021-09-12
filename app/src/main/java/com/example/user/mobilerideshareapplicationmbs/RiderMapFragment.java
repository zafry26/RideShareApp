package com.example.user.mobilerideshareapplicationmbs;


import android.*;
import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.user.mobilerideshareapplicationmbs.NotificationGenerator.DriverAcceptRequestNotification;
import com.example.user.mobilerideshareapplicationmbs.NotificationGenerator.DriverArriveNotification;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bumptech.glide.gifdecoder.GifHeaderParser.TAG;


public class RiderMapFragment extends Fragment implements OnMapReadyCallback, View.OnClickListener {


    GoogleMap mMap;
    SupportMapFragment supportMapFragment;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    FusedLocationProviderClient fusedLocationProviderClient;

    private Button riderRequest;

    private LatLng pickupLocation;
    private LatLng destinationLatLong;

    private Boolean requestBol = false;
    private Boolean mRequestingLocation = false;

    private Marker pickupMarker;

    private ConstraintLayout mDriverInfo;

    private ImageView driverImageView;
    private ImageView driverCarViewInfo;

    private TextView driverNameView;
    private TextView driverPhoneView;
    private TextView driverPlatNumberView;
    private TextView driverCarTypeInfo;

    private String riderDestination;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_rider_map, container, false);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

        supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map2);

        supportMapFragment.getMapAsync(this);

        destinationLatLong = new LatLng(0.0, 0.0);

        mDriverInfo = view.findViewById(R.id.driver_info_layout);

        riderRequest = view.findViewById(R.id.rider_request);
        riderRequest.setOnClickListener(this);

        driverImageView = view.findViewById(R.id.driverImageViewInfo);
        driverCarViewInfo = view.findViewById(R.id.driverCarViewInfo);

        driverNameView = view.findViewById(R.id.driverNameInfo);
        driverPhoneView = view.findViewById(R.id.driverPhoneInfo);
        driverPlatNumberView = view.findViewById(R.id.driverPlatNumberInfo);
        driverCarTypeInfo = view.findViewById(R.id.driverCarTypeInfo);

        return view;
    }

    @Override
    public void onClick(View view) {

        if (requestBol) {
            try {
                endRide();
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

        } else {
            try {

                requestBol = true;

                String user_ID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference databaseRoute = FirebaseDatabase.getInstance().getReference("Route");

                GeoFire geoFire = new GeoFire(databaseRoute);
                geoFire.setLocation(user_ID, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

                pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Location"));

                riderRequest.setText("Finding Nearest Driver Available");

                getClosestDriver();
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment) getActivity().getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment_rider);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                riderDestination = place.getName().toString();
                destinationLatLong = place.getLatLng();
            }

            @Override
            public void onError(Status status) {

            }
        });
    }


    private int radius = 1;
    private boolean driverFound = false;
    private String driverFoundID;
    GeoQuery geoQuery;

    private void getClosestDriver() {

        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference("Driver Availability");

        GeoFire geoFire = new GeoFire(driverLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && requestBol) {
                    driverFound = true;
                    driverFoundID = key;

                    DatabaseReference databaseDriver = FirebaseDatabase.getInstance().getReference("Driver").child(driverFoundID).child("Customer Request");

                    String riderID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    HashMap map = new HashMap();
                    map.put("Customer Rider ID", riderID);
                    map.put("Rider Destination", riderDestination);
                    map.put("Rider Destination Latitude", destinationLatLong.latitude);
                    map.put("Rider Destination Longitude", destinationLatLong.longitude);
                    databaseDriver.updateChildren(map);

                    getDriverLocation();
                    getHasRideEnded();
                    getAssignedDriverInfo();
                    riderRequest.setText("Finding Nearest Driver Location");
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

                if (!driverFound) {
                    radius++;
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }


    private Marker mDriverMarker;
    private DatabaseReference databaseDriverWorking;
    private ValueEventListener driverLocationListener;

    private void getDriverLocation() {

        databaseDriverWorking = FirebaseDatabase.getInstance().getReference("Driver Working").child(driverFoundID).child("l");

        driverLocationListener = databaseDriverWorking.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBol) {

                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLong = 0;
                    riderRequest.setText("Driver Found");

                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationLong = Double.parseDouble(map.get(1).toString());
                    }

                    LatLng driverLatLong = new LatLng(locationLat, locationLong);

                    if (mDriverMarker != null) {
                        mDriverMarker.remove();
                    }

                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatLong.latitude);
                    loc2.setLongitude(driverLatLong.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if (distance < 100) {

                        riderRequest.setText("Driver Arrive");
                        DriverArriveNotification.openActivityNotification(getActivity().getApplicationContext());
                    } else {
                        distance = distance / 1000;
                        String stringDistance = String.valueOf(distance);
                        riderRequest.setText("Driver Found: " + stringDistance.substring(0, Math.min(stringDistance.length(), 5)) + " km");
                    }

                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLong).title("Your Driver"));

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private DatabaseReference getAssignedDriverInfoDB3;
    private ValueEventListener getAssignedDriverInfoDB3Listener;

    private void getAssignedDriverInfo() {

        mDriverInfo.setVisibility(View.VISIBLE);

        DatabaseReference getAssignedDriverInfoDB = FirebaseDatabase.getInstance().getReference("Driver").child(driverFoundID);

        getAssignedDriverInfoDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if (map.get("User Name") != null) {
                        driverNameView.setText(map.get("User Name").toString());
                    }

                    if (map.get("Contact Number") != null) {
                        driverPhoneView.setText(map.get("Contact Number").toString());
                    }

                    if (map.get("Profile Images Url") != null) {

                        Glide.with(getActivity().getApplication()).load(map.get("Profile Images Url").toString()).into(driverImageView);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        DatabaseReference getAssignedDriverInfoDB2 = FirebaseDatabase.getInstance().getReference("Car Information").child(driverFoundID);

        getAssignedDriverInfoDB2.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if (map.get("Car Plat Number") != null) {
                        driverPlatNumberView.setText(map.get("Car Plat Number").toString());
                    }

                    if (map.get("Car Images Url") != null) {

                        Glide.with(getActivity().getApplication()).load(map.get("Car Images Url").toString()).into(driverCarViewInfo);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        getAssignedDriverInfoDB3 = FirebaseDatabase.getInstance().getReference("Driver").child(driverFoundID).child("Customer Request");

        getAssignedDriverInfoDB3Listener = getAssignedDriverInfoDB3.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if (map.get("RideSharing Status") != null) {

                        driverCarTypeInfo.setText(map.get("RideSharing Status").toString());
                        DriverAcceptRequestNotification.openActivityNotification(getActivity().getApplicationContext());

                        Toast.makeText(getActivity(), "Please wait until your driver arrived", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private DatabaseReference driverDriveHasEnded;
    private ValueEventListener driverDriveHasEndedListener;

    private void getHasRideEnded() {

        driverDriveHasEnded = FirebaseDatabase.getInstance().getReference("Driver").child(driverFoundID).child("Customer Request").child("Customer Rider ID");

        driverDriveHasEndedListener = driverDriveHasEnded.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                } else {
                    endRide();

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void endRide() {

        geoQuery.removeAllListeners();

        if (driverLocationListener != null) {
            databaseDriverWorking.removeEventListener(driverLocationListener);
        }
        if (driverDriveHasEndedListener != null) {
            driverDriveHasEnded.removeEventListener(driverDriveHasEndedListener);
        }

        if (getAssignedDriverInfoDB3Listener != null) {
            getAssignedDriverInfoDB3.removeEventListener(getAssignedDriverInfoDB3Listener);
        }

        requestBol = false;

        if (driverFoundID != null) {

            DatabaseReference databaseDriver = FirebaseDatabase.getInstance().getReference("Driver").child(driverFoundID).child("Customer Request");
            databaseDriver.child("Customer Rider ID").removeValue();
            databaseDriver.child("Rider Destination").removeValue();
            databaseDriver.child("Rider Destination Latitude").removeValue();
            databaseDriver.child("Rider Destination Longitude").removeValue();
            databaseDriver.child("RideSharing Status").removeValue();
            driverFoundID = null;
        }

        riderDestination = "";

        driverFound = false;
        radius = 1;

        String user_ID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference databaseRoute = FirebaseDatabase.getInstance().getReference("Route");

        GeoFire geoFire = new GeoFire(databaseRoute);
        geoFire.removeLocation(user_ID);

        if (pickupMarker != null) {
            pickupMarker.remove();
        }
        if (mDriverMarker != null) {
            mDriverMarker.remove();
        }

        mDriverInfo.setVisibility(View.GONE);
        driverImageView.setImageResource(R.drawable.user_default_image);
        driverCarViewInfo.setImageResource(R.drawable.user_default_image);
        driverNameView.setText("");
        driverPhoneView.setText("");
        driverPlatNumberView.setText("");
        driverCarTypeInfo.setText("");

        riderRequest.setText("Request Ride");
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            } else {
                checkLocationPermission();
            }
        } else {
            fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                Log.i("RiderMapFragment", "Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastLocation = location;
                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }

                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(10));
            }
        }
    };

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 1;

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {

                new AlertDialog.Builder(getActivity())
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();
            } else {

                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                        fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    Toast.makeText(getActivity(), "Permission Denied", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    private void connectRider() {

        checkLocationPermission();

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            } else {
                checkLocationPermission();
            }
        } else {
            fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }

    }

    private void disconnectRider() {

        fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRequestingLocation) {
            mRequestingLocation = false;
            disconnectRider();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mRequestingLocation) {

        }
        else {
            mRequestingLocation = true;
            connectRider();
        }
    }
}