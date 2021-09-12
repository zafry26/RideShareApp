package com.example.user.mobilerideshareapplicationmbs;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.support.design.widget.TabLayout;
import android.widget.TextView;
import android.widget.VideoView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

public class Login_Signup extends AppCompatActivity implements Login_Page.OnFragmentInteractionListener,Signup_Page.OnFragmentInteractionListener {

    private VideoView nVideoView;
    Fragment fragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login__signup);

        nVideoView = (VideoView) findViewById(R.id.loginSignup_bgVideo);

        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.bg_video);

        nVideoView.setVideoURI(uri);
        nVideoView.start();

        nVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        });


        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        PagerAdapter pagerAdapter = new PagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tablayout);
        tabLayout.setupWithViewPager(viewPager);


        Intent intent = getIntent();
        String message = intent.getStringExtra("Extra Message");

    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}

