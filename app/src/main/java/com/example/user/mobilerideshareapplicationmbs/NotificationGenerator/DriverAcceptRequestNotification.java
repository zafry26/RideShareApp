package com.example.user.mobilerideshareapplicationmbs.NotificationGenerator;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.example.user.mobilerideshareapplicationmbs.R;
import com.example.user.mobilerideshareapplicationmbs.RiderDashboard;
import com.example.user.mobilerideshareapplicationmbs.RiderMapFragment;

public class DriverAcceptRequestNotification {

    private static final int NOTIFICATION_ID_OPEN_ACTIVITY = 9;

    public static void openActivityNotification(Context context){
        NotificationCompat.Builder nc = new NotificationCompat.Builder(context);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notifyIntent = new Intent(context, RiderDashboard.class);

        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context,0,notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        nc.setContentIntent(pendingIntent);

        nc.setSmallIcon(R.mipmap.ic_launcher);
        nc.setAutoCancel(true);
        nc.setContentTitle("Your Request Accepted");
        nc.setContentText("Click Here To Open");

        nm.notify(NOTIFICATION_ID_OPEN_ACTIVITY, nc.build());
    }
}
