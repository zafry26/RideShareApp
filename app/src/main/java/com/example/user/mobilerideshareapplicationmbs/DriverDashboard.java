package com.example.user.mobilerideshareapplicationmbs;

import android.app.ProgressDialog;
import android.support.v4.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class DriverDashboard extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DatabaseReference databaseDriver;
    private FirebaseAuth firebaseAuth;

    private String user_id;
    private ImageView driver_img;
    private TextView driverNameField;
    private TextView driverUserTypeField;
    private String driverName;
    private String driverUserType;
    private String profileImageUrl;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_driver_dashboard);
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
        databaseDriver = FirebaseDatabase.getInstance().getReference("Driver").child(user_id);


        View header = navigationView.getHeaderView(0);


        driver_img = header.findViewById(R.id.driver_profile_image);
        driverNameField = header.findViewById(R.id.driver_user_name);
        driverUserTypeField = header.findViewById(R.id.driver_user_type);

        DriverMapFragment driverMapFragment = new DriverMapFragment();
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().replace(R.id.driver_dashboard_layout, driverMapFragment).commit();

        displayNavHeaderInformation();
    }


    private void displayNavHeaderInformation() {

        databaseDriver.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if (map.get("User Name") != null) {
                        driverName = map.get("User Name").toString();
                        driverNameField.setText(driverName);
                    }

                    if (map.get("User Type") != null) {
                        driverUserType = map.get("User Type").toString();
                        driverUserTypeField.setText(driverUserType);
                    }

                    if (map.get("Profile Images Url") != null) {
                        profileImageUrl = map.get("Profile Images Url").toString();
                        Glide.with(getApplication()).load(profileImageUrl).into(driver_img);
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

        if (id == R.id.driver_homepage) {
            // Handle the manage action
            DriverMapFragment driverMapFragment = new DriverMapFragment();
            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction().replace(R.id.driver_dashboard_layout, driverMapFragment).commit();



        } else if (id == R.id.driver_manage_profile) {
            // Handle the manage action
            DriverProfileFragment driverProfile = new DriverProfileFragment();
            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction().replace(R.id.driver_dashboard_layout, driverProfile).commit();


        } else if (id == R.id.driver_car_information) {

            DriverCarFragment driverCarFragment = new DriverCarFragment();
            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction().replace(R.id.driver_dashboard_layout, driverCarFragment).commit();

        } else if (id == R.id.driver_your_trips) {

            HistoryFragment historyFragment = new HistoryFragment();
            Bundle bundle = new Bundle();
            bundle.putString("customerOrDriver", "Driver");
            historyFragment.setArguments(bundle);
            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction().replace(R.id.driver_dashboard_layout, historyFragment).commit();

        } else if (id == R.id.driver_sign_out) {
            
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
        Intent intent = new Intent(DriverDashboard.this, Homepage.class);
        startActivity(intent);
        finish();
    }
}
