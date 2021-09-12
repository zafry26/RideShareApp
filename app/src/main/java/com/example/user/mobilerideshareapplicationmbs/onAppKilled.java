package com.example.user.mobilerideshareapplicationmbs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.firebase.geofire.GeoFire;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.security.Provider;

public class onAppKilled extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverAvailableDb = FirebaseDatabase.getInstance().getReference("Driver Availability");
        GeoFire geoFireDriverAvailability = new GeoFire(driverAvailableDb);
        geoFireDriverAvailability.removeLocation(user_id);
    }
}
