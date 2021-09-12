package com.example.user.mobilerideshareapplicationmbs.LeakCanary;

import android.app.Application;
import android.content.Context;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

public class MyWatcher extends Application {

    private RefWatcher refWatcher;

    public static RefWatcher getRefWatcher(Context context){
        MyWatcher myWatcher = (MyWatcher) context.getApplicationContext();
        return myWatcher.refWatcher;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        refWatcher = LeakCanary.install(this);
    }
}
