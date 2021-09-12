package com.example.user.mobilerideshareapplicationmbs;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.example.user.mobilerideshareapplicationmbs.RecyclerView.HistoryAdapter;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class SingleHistoryFragment extends Fragment implements OnMapReadyCallback, RoutingListener
{
    private String rideId, currentUserId, customerId, driverId, userDriverOrCustomer;

    private TextView rideLocation;
    private TextView rideDistance;
    private TextView rideDate;
    private TextView userName;
    private TextView userPhone;
    private TextView ridesharePrice;
    private TextView paymentMethod;

    private ImageView userImage;

    private RatingBar mRatingBar;

    private Button mPay;
    private Button mPay2;

    private DatabaseReference historyRideInfoDb;
    private DatabaseReference paymentRideDB;

    private LatLng destinationLatLng, pickupLatLng, lastLocation;
    private String distance;
    private Double ridePrice;

    private boolean customerPaid = false;

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view=  inflater.inflate(R.layout.fragment_single_history, container, false);

        Intent intent = new Intent(getActivity(), PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        getActivity().startService(intent);

        polylines = new ArrayList<>();

        Bundle bundle = this.getArguments();
        if (bundle != null){
            rideId = bundle.getString("rideId");
        }

        mMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map3);
        mMapFragment.getMapAsync(this);


        rideLocation =  view.findViewById(R.id.rideLocation);
        rideDistance =  view.findViewById(R.id.rideDistance);
        rideDate =  view.findViewById(R.id.rideDate);
        userName =  view.findViewById(R.id.userName);
        userPhone =  view.findViewById(R.id.userPhone);
        ridesharePrice = view.findViewById(R.id.ridePrice);
        paymentMethod = view.findViewById(R.id.paymentMethod);

        userImage =  view.findViewById(R.id.userImage);

        mRatingBar =  view.findViewById(R.id.ratingBar);

        mPay = view.findViewById(R.id.pay);
        mPay2 = view.findViewById(R.id.pay2);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        historyRideInfoDb = FirebaseDatabase.getInstance().getReference("Ride History").child(rideId);

        paymentRideDB = FirebaseDatabase.getInstance().getReference("Payment").child(rideId);

        getRideInformation();

        return view;
    }



    private void getRideInformation() {
        historyRideInfoDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for (DataSnapshot child:dataSnapshot.getChildren()){
                        if (child.getKey().equals("Rider")){
                            customerId = child.getValue().toString();
                            if(!customerId.equals(currentUserId)){
                                userDriverOrCustomer = "Driver";
                                getUserInformation("Rider", customerId);
                                checkCashPayment();
                            }
                        }
                        if (child.getKey().equals("Driver")){
                            driverId = child.getValue().toString();
                            if(!driverId.equals(currentUserId)){
                                userDriverOrCustomer = "Rider";
                                getUserInformation("Driver", driverId);
                                displayCustomerRelatedObjects();
                            }
                        }
                        if (child.getKey().equals("Timestamp")){
                            rideDate.setText(getDate(Long.valueOf(child.getValue().toString())));
                        }
                        if (child.getKey().equals("Rating")){
                            mRatingBar.setRating(Integer.valueOf(child.getValue().toString()));

                        }
                        if (child.getKey().equals("Distance")){
                            distance = child.getValue().toString();
                            rideDistance.setText(distance.substring(0, Math.min(distance.length(), 5)) + " km");
                            ridePrice = Double.valueOf(distance) * 2.5;
                            ridesharePrice.setText(String.format("RM %.2f", ridePrice));

                        }
                        if (child.getKey().equals("Destination")){
                            rideLocation.setText(child.getValue().toString());
                        }
                        if (child.getKey().equals("Location")){
                            pickupLatLng = new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()), Double.valueOf(child.child("from").child("lng").getValue().toString()));
                            destinationLatLng = new LatLng(Double.valueOf(child.child("to").child("lat").getValue().toString()), Double.valueOf(child.child("to").child("lng").getValue().toString()));
                            lastLocation = new LatLng(Double.valueOf(child.child("last location").child("lat").getValue().toString()), Double.valueOf(child.child("last location").child("lng").getValue().toString()));

                            if (lastLocation != new LatLng(0,0)) {
                                getRouteToMarker();
                            }

                        }
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        paymentRideDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for (DataSnapshot child:dataSnapshot.getChildren()) {
                        if (child.getKey().equals("Payment Type")) {
                            paymentMethod.setText(child.getValue().toString());
                            mPay.setEnabled(false);
                            mPay2.setEnabled(false);
                        }
                    }
                }
                else {
                    mPay.setEnabled(true);
                    mPay2.setEnabled(true);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void displayCustomerRelatedObjects() {
        mRatingBar.setVisibility(View.VISIBLE);
        mPay.setVisibility(View.VISIBLE);
        mPay2.setVisibility(View.VISIBLE);
        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                historyRideInfoDb.child("Rating").setValue(rating);
                DatabaseReference mDriverRatingDb = FirebaseDatabase.getInstance().getReference().child("Driver").child(driverId).child("Rating");
                mDriverRatingDb.child(rideId).setValue(rating);
            }
        });

        mPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                payPalPayment();
            }
        });
        
        mPay2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new AlertDialog.Builder(getActivity())
                        .setTitle("Rider Pay Via Cash")
                        .setMessage("You set to pay via cash. Press OK to confirm")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                cashPayment();
                            }
                        })
                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(getActivity().getApplicationContext(), "Payment unsuccessful", Toast.LENGTH_LONG).show();

                            }
                        })
                        .create()
                        .show();
            }
        });
    }

    private void cashPayment() {

        HashMap map = new HashMap();
        map.put("Driver", driverId);
        map.put("Rider", customerId);
        map.put("Payment Type", "Cash");
        map.put("Payment Date", getCurrentTimestamp());
        map.put("Payment Amount", ridePrice);
        map.put("Payment Status", "Pending");
        paymentRideDB.updateChildren(map);

        mPay.setEnabled(false);
        mPay2.setEnabled(false);
    }


    private ValueEventListener checkCashPaymentListener;

    private void checkCashPayment(){
        checkCashPaymentListener = paymentRideDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        if (child.getValue().equals("Pending")) {

                            new AlertDialog.Builder(getActivity())
                                    .setTitle("Rider Pay Via Cash")
                                    .setMessage("Rider set to pay via cash. Press OK if payment is accepted")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {

                                            HashMap map = new HashMap();
                                            map.put("Payment Status", "Accepted");
                                            paymentRideDB.updateChildren(map);

                                            mPay2.setEnabled(false);
                                            mPay.setEnabled(false);

                                            if (checkCashPaymentListener != null) {
                                                paymentRideDB.removeEventListener(checkCashPaymentListener);
                                            }
                                        }
                                    })
                                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Toast.makeText(getActivity().getApplicationContext(), "Please call rider to claim your payment", Toast.LENGTH_LONG).show();

                                        }
                                    })
                                    .create()
                                    .show();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private int PAYPAL_REQUEST_CODE = 1;
    private static PayPalConfiguration config = new PayPalConfiguration()
            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
            .clientId(PayPalConfig.PAYPAL_CLIENT_ID);


    private void payPalPayment() {
        PayPalPayment payment = new PayPalPayment(new BigDecimal(ridePrice), "MYR", "Uber Ride",
                PayPalPayment.PAYMENT_INTENT_SALE);

        Intent intent = new Intent(getActivity(), PaymentActivity.class);

        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);

        startActivityForResult(intent, PAYPAL_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PAYPAL_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
                PaymentConfirmation paymentConfirmation = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
                if (paymentConfirmation != null){
                    try {
                        JSONObject jsonObject = new JSONObject(paymentConfirmation.toJSONObject().toString());

                        String paymentResponse = jsonObject.getJSONObject("response").getString("state");

                        if (paymentResponse.equals("approved")){
                            Toast.makeText(getActivity().getApplicationContext(), "Payment Successfull", Toast.LENGTH_LONG).show();

                            HashMap map = new HashMap();
                            map.put("Driver", driverId);
                            map.put("Rider", customerId);
                            map.put("Payment Type", "Paypal");
                            map.put("Payment Date", getCurrentTimestamp());
                            map.put("Payment Amount", ridePrice);
                            paymentRideDB.updateChildren(map);

                            mPay.setEnabled(false);
                            mPay2.setEnabled(false);
                        }
                    }
                    catch (JSONException e){
                        e.printStackTrace();
                    }
                }


            }else{
                Toast.makeText(getActivity().getApplicationContext(), "Payment unsuccessful", Toast.LENGTH_LONG).show();
            }
        }
    }

    private Long getCurrentTimestamp() {
        Long timestamp = System.currentTimeMillis()/1000;
        return timestamp;
    }

    @Override
    public void onDestroy() {
        getActivity().stopService(new Intent(getActivity(), PayPalService.class));
        super.onDestroy();
    }






    private void getUserInformation(String otherUserDriverOrCustomer, String otherUserId) {
        DatabaseReference mOtherUserDB = FirebaseDatabase.getInstance().getReference().child(otherUserDriverOrCustomer).child(otherUserId);
        mOtherUserDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("User Name") != null){
                        userName.setText(map.get("User Name").toString());
                    }
                    if(map.get("Contact Number") != null){
                        userPhone.setText(map.get("Contact Number").toString());
                    }
                    if(map.get("Profile Images Url") != null){
                        Glide.with(getActivity().getApplication()).load(map.get("Profile Images Url").toString()).into(userImage);
                    }
                }

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private String getDate(Long time) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(time*1000);
        String date = DateFormat.format("MM-dd-yyyy hh:mm", cal).toString();
        return date;
    }


    private void getRouteToMarker() {

            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(false)
                    .waypoints(pickupLatLng, lastLocation)
                    .build();
            routing.execute();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap=googleMap;
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

            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(pickupLatLng);
            builder.include(lastLocation);
            LatLngBounds bounds = builder.build();

            int width = getResources().getDisplayMetrics().widthPixels;
            int padding = (int) (width * 0.2);

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);

            mMap.animateCamera(cameraUpdate);

            mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup location"));
            mMap.addMarker(new MarkerOptions().position(lastLocation).title("Destination"));

            if (polylines.size() > 0) {
                for (Polyline poly : polylines) {
                    poly.remove();
                }
            }

            polylines = new ArrayList<>();
            //add route(s) to the map.
            for (int i = 0; i < route.size(); i++) {

                //In case of more than 5 alternative routes
                int colorIndex = i % COLORS.length;

                PolylineOptions polyOptions = new PolylineOptions();
                polyOptions.color(getResources().getColor(COLORS[colorIndex]));
                polyOptions.width(10 + i * 3);
                polyOptions.addAll(route.get(i).getPoints());
                Polyline polyline = mMap.addPolyline(polyOptions);
                polylines.add(polyline);

                Toast.makeText(getActivity().getApplicationContext(), "Route " + (i + 1) + ": distance - " + route.get(i).getDistanceValue() + ": duration - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();
            }
    }
    @Override
    public void onRoutingCancelled() {
    }
    private void erasePolylines(){
        for(Polyline line : polylines){
            line.remove();
        }
        polylines.clear();
    }

    @Override
    public void onDetach() {
        super.onDetach();

    }
}
