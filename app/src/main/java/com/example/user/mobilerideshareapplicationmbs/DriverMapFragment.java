package com.example.user.mobilerideshareapplicationmbs;


import android.*;
import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
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
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.example.user.mobilerideshareapplicationmbs.NotificationGenerator.DriverArriveNotification;
import com.example.user.mobilerideshareapplicationmbs.NotificationGenerator.RiderRequestNotification;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DriverMapFragment extends Fragment implements OnMapReadyCallback, RoutingListener {

    GoogleMap mMap;
    SupportMapFragment supportMapFragment;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    FusedLocationProviderClient fusedLocationProviderClient;

    private Boolean mRequestingLocation = false;

    private String customerId = "";
    private String destination;
    private Boolean zoomDriving = false;

    private LatLng destinationLatLong;

    private ConstraintLayout mCustomerInfo;

    private ImageView riderImageView;

    private TextView riderNameView;
    private TextView riderPhoneView;
    private TextView riderDestinationView;

    private Switch driverWorkingSwitch;

    private Button mRideStatus;
    private Button mRideStatusCancel;
    private Button mRideStatus2;
    private Button mRideStatus3;

    private float rideDistance;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_driver_map, container, false);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

        polylines = new ArrayList<>();

        supportMapFragment = (SupportMapFragment)getChildFragmentManager().findFragmentById(R.id.map1);
        supportMapFragment.getMapAsync(this);

        mCustomerInfo = view.findViewById(R.id.rider_info_layout);

        riderImageView = view.findViewById(R.id.riderImageViewInfo);
        riderNameView = view.findViewById(R.id.riderNameInfo);
        riderPhoneView = view.findViewById(R.id.riderPhoneInfo);
        riderDestinationView = view.findViewById(R.id.riderDestinationInfo);

        mRideStatus = view.findViewById(R.id.button_driver_layout_accept);
        mRideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                getRouteToMarker(riderLatLong);
                zoomDriving = true;
                        
                String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference databaseDriver = FirebaseDatabase.getInstance().getReference("Driver").child(user_id).child("Customer Request");

                String acceptRequest = "Request Accepted";

                HashMap map = new HashMap();
                map.put("RideSharing Status", acceptRequest);
                databaseDriver.updateChildren(map);

                mRideStatus.setVisibility(View.GONE);
                mRideStatus2.setVisibility(View.VISIBLE);
                mRideStatusCancel.setText("Cancel Ride");
                Toast.makeText(getActivity(), "Ride to rider pickup location", Toast.LENGTH_LONG).show();
            }
        });

        mRideStatus2 = view.findViewById(R.id.button_driver_layout_driving);
        mRideStatus2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (destinationLatLong.latitude != 0.0 && destinationLatLong.longitude != 0.0) {
                    clearPolylines();
                    getRouteToMarker(destinationLatLong);
                    Toast.makeText(getActivity(), "Ride to rider destination", Toast.LENGTH_LONG).show();
                } else {
                    clearPolylines();
                }

                mRideStatus2.setVisibility(View.GONE);
                mRideStatus3.setVisibility(View.VISIBLE);
            }
        });

        mRideStatus3 = view.findViewById(R.id.button_driver_layout_rideFinishing);
        mRideStatus3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

               recordRide();
               endRide();
            }
        });

        mRideStatusCancel = view.findViewById(R.id.button_driver_layout_cancel);
        mRideStatusCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                endRide();

            }
        });

        driverWorkingSwitch = view.findViewById(R.id.driver_status);
        driverWorkingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked){
                    connectDriver();
                }
                else{
                    disconnectDriver();
                }
            }
        });

        getAssignedRider();


        return view;
    }


    private void getAssignedRider() {

        String driverID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedRider = FirebaseDatabase.getInstance().getReference("Driver").child(driverID).child("Customer Request").child("Customer Rider ID");

        assignedRider.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    customerId = dataSnapshot.getValue().toString();

                    getAssignedRiderPickupLocation();
                    getAssignedRiderPickupDestination();
                    getAssignedRiderInfo();
                }
                else {
                    endRide();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }



    private Marker pickupMarker;
    DatabaseReference assignedRiderPickupLocation;
    private ValueEventListener assignedRiderPickupLocationlistener;
    LatLng riderLatLong;

    private void getAssignedRiderPickupLocation() {

        assignedRiderPickupLocation = FirebaseDatabase.getInstance().getReference("Route").child(customerId).child("l");

        assignedRiderPickupLocationlistener = assignedRiderPickupLocation.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !customerId.equals("")){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLong = 0;

                    if (map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null){
                        locationLong = Double.parseDouble(map.get(1).toString());
                    }

                    riderLatLong = new LatLng(locationLat, locationLong);

                    pickupMarker = mMap.addMarker(new MarkerOptions().position(riderLatLong).title("Pickup Location"));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void getRouteToMarker(LatLng driverLatLong) {

        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), driverLatLong)
                .build();
        routing.execute();
    }



    DatabaseReference assignedRiderPickupDestination;
    private ValueEventListener assignedRiderPickupDestinationlistener;

    private void getAssignedRiderPickupDestination() {

        String driverID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        assignedRiderPickupDestination = FirebaseDatabase.getInstance().getReference("Driver").child(driverID).child("Customer Request");

        assignedRiderPickupDestinationlistener = assignedRiderPickupDestination.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("Rider Destination") != null) {
                        destination = map.get("Rider Destination").toString();

                        riderDestinationView.setText("Destination: " + destination);
                    } else {
                        
                        riderDestinationView.setText("Destination: --");
                    }

                    Double destinationLat = 0.0;
                    Double destinationLong = 0.0;

                    if (map.get("Rider Destination Latitude") != null) {
                        destinationLat = Double.valueOf(map.get("Rider Destination Latitude").toString());
                    }
                    if (map.get("Rider Destination Longitude") != null) {
                        destinationLong = Double.valueOf(map.get("Rider Destination Longitude").toString());
                        destinationLatLong = new LatLng(destinationLat, destinationLong);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }



    private void getAssignedRiderInfo() {

        mCustomerInfo.setVisibility(View.VISIBLE);
        mRideStatus2.setVisibility(View.GONE);
        mRideStatus3.setVisibility(View.GONE);

        DatabaseReference getAssignedRiderInfoDB = FirebaseDatabase.getInstance().getReference("Rider").child(customerId);

        getAssignedRiderInfoDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if (map.get("User Name") != null) {
                        riderNameView.setText(map.get("User Name").toString());
                    }

                    if (map.get("Contact Number") != null) {
                        riderPhoneView.setText(map.get("Contact Number").toString());
                    }

                    if (map.get("Profile Images Url") != null) {

                        Glide.with(getActivity().getApplication()).load(map.get("Profile Images Url").toString()).into(riderImageView);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void endRide(){

        clearPolylines();

        String user_ID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference databaseDriver = FirebaseDatabase.getInstance().getReference("Driver").child(user_ID).child("Customer Request");
        databaseDriver.child("Customer Rider ID").removeValue();
        databaseDriver.child("Rider Destination").removeValue();
        databaseDriver.child("Rider Destination Latitude").removeValue();
        databaseDriver.child("Rider Destination Longitude").removeValue();
        databaseDriver.child("RideSharing Status").removeValue();


        DatabaseReference databaseRoute = FirebaseDatabase.getInstance().getReference("Route");

        GeoFire geoFire = new GeoFire(databaseRoute);
        geoFire.removeLocation(user_ID);

        rideDistance =0;

        zoomDriving = false;

        customerId = "";

        if (pickupMarker != null) {
            pickupMarker.remove();
        }

        if (assignedRiderPickupLocationlistener != null) {
            assignedRiderPickupLocation.removeEventListener(assignedRiderPickupLocationlistener);
        }

        if (assignedRiderPickupDestinationlistener != null) {
            assignedRiderPickupDestination.removeEventListener(assignedRiderPickupDestinationlistener);
        }

        mRideStatus.setVisibility(View.VISIBLE);
        mRideStatusCancel.setText("Reject Request");

        mCustomerInfo.setVisibility(View.GONE);
        riderImageView.setImageResource(R.drawable.user_default_image);
        riderNameView.setText("");
        riderPhoneView.setText("");
        riderDestinationView.setText("Destination: --" );

    }

    private void recordRide(){

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference("Driver").child(userId).child("History");
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference("Rider").child(customerId).child("History");
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("Ride History");
        String requestId = historyRef.push().getKey();
        driverRef.child(requestId).setValue(true);
        customerRef.child(requestId).setValue(true);

        HashMap map = new HashMap();
        map.put("Driver", userId);
        map.put("Rider", customerId);
        map.put("Rating", 0);
        map.put("Timestamp", getCurrentTimestamp());
        map.put("Destination", destination);
        map.put("Location/from/lat", riderLatLong.latitude);
        map.put("Location/from/lng", riderLatLong.longitude);
        map.put("Location/to/lat", destinationLatLong.latitude);
        map.put("Location/to/lng", destinationLatLong.longitude);
        map.put("Location/last location/lat", mLastLocation.getLatitude());
        map.put("Location/last location/lng", mLastLocation.getLongitude());
        map.put("Distance", rideDistance);
        historyRef.child(requestId).updateChildren(map);

        Toast.makeText(getActivity(), "Proceed to history and payment", Toast.LENGTH_LONG).show();
    }

    private Long getCurrentTimestamp() {
        Long timestamp = System.currentTimeMillis()/1000;
        return timestamp;
    }



    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap=googleMap;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            } else {
                checkLocationPermission();
            }
        }
    }

    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {

                if(!customerId.equals("")){
                    rideDistance += mLastLocation.distanceTo(location)/1000; }

                mLastLocation = location;
                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }

                if (zoomDriving) {

                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(18));
                }
                else{
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(10));
                }


                String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

                DatabaseReference databaseDriverAvailability = FirebaseDatabase.getInstance().getReference("Driver Availability");
                DatabaseReference databaseDriverWorking = FirebaseDatabase.getInstance().getReference("Driver Working");

                GeoFire geoFireDriverAvailability = new GeoFire(databaseDriverAvailability);
                GeoFire geoFireDriverWorking = new GeoFire(databaseDriverWorking);

                switch (customerId){

                        case "":
                        geoFireDriverWorking.removeLocation(user_id);
                        geoFireDriverAvailability.setLocation(user_id, new GeoLocation(location.getLatitude(), location.getLongitude()));
                        break;

                        default:
                            geoFireDriverAvailability.removeLocation(user_id);
                            geoFireDriverWorking.setLocation(user_id, new GeoLocation(location.getLatitude(), location.getLongitude()));
                            break;
                }
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

                                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions,grantResults);
        switch (requestCode) {

            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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



    private void connectDriver(){

        checkLocationPermission();
        fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);
    }

    private void disconnectDriver(){
        if (fusedLocationProviderClient != null) {

            fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        }

        String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverAvailableDb = FirebaseDatabase.getInstance().getReference("Driver Availability");
        GeoFire geoFireDriverAvailability = new GeoFire(driverAvailableDb);
        geoFireDriverAvailability.removeLocation(user_id);

    }



    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};

    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(getActivity(), "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();

        for (int i = 0; i <route.size(); i++) {

            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getActivity().getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {

    }

    private void clearPolylines(){
        for(Polyline line : polylines){
            line.remove();
        }
        polylines.clear();

    }
}
