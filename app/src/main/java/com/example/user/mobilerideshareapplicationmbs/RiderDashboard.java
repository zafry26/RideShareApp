package com.example.user.mobilerideshareapplicationmbs;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class RiderDashboard extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DatabaseReference databaseRider;
    private FirebaseAuth firebaseAuth;

    private String user_id;
    private ImageView rider_img;
    private TextView riderNameField;
    private TextView riderUserTypeField;
    private String riderName;
    private String riderUserType;
    private String profileImageUrl;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_dashboard);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        progressDialog = new ProgressDialog(this);

        firebaseAuth = FirebaseAuth.getInstance();

        user_id =firebaseAuth.getCurrentUser().getUid();
        databaseRider = FirebaseDatabase.getInstance().getReference("Rider").child(user_id);


        View header = navigationView.getHeaderView(0);


        rider_img = header.findViewById(R.id.rider_profile_image);
        riderNameField = header.findViewById(R.id.rider_user_name);
        riderUserTypeField = header.findViewById(R.id.rider_user_type);

        RiderMapFragment riderMapFragment = new RiderMapFragment();
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().replace(R.id.rider_dashboard_layout, riderMapFragment).commit();

        displayNavHeaderInformation();
    }

    private void displayNavHeaderInformation() {

        databaseRider.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if (map.get("User Name") != null) {
                        riderName = map.get("User Name").toString();
                        riderNameField.setText(riderName);
                    }

                    if (map.get("User Type") != null) {
                        riderUserType = map.get("User Type").toString();
                        riderUserTypeField.setText(riderUserType);
                    }

                    if (map.get("Profile Images Url") != null) {
                        profileImageUrl = map.get("Profile Images Url").toString();
                        Glide.with(getApplication()).load(profileImageUrl).into(rider_img);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    @Override
    public void onBackPressed() {


    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.rider_homepage) {

            PlaceAutocompleteFragment f = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment_rider);
            if (f != null){

                getFragmentManager().beginTransaction().remove(f).commit();
            }

            RiderMapFragment riderMapFragment = new RiderMapFragment();
            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction().replace(R.id.rider_dashboard_layout, riderMapFragment).commit();

        } else if (id == R.id.rider_manage_profile) {

            RiderProfileFragment riderProfile = new RiderProfileFragment();
            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction().replace(R.id.rider_dashboard_layout, riderProfile).commit();

        } else if (id == R.id.rider_your_trips) {

            HistoryFragment historyFragment = new HistoryFragment();
            Bundle bundle = new Bundle();
            bundle.putString("customerOrDriver", "Rider");
            historyFragment.setArguments(bundle);
            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction().replace(R.id.rider_dashboard_layout, historyFragment).commit();

        } else if (id == R.id.rider_sign_out) {

            Logout();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void Logout() {
        progressDialog.setMessage("Sign out user.....");
        progressDialog.show();
        firebaseAuth.signOut();
        finish();
        progressDialog.dismiss();
        Intent intent = new Intent(RiderDashboard.this , Homepage.class);
        startActivity(intent);
        finish();
    }
}
