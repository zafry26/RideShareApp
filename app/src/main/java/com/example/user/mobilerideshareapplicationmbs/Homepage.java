package com.example.user.mobilerideshareapplicationmbs;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;

public class Homepage extends AppCompatActivity {

    VideoView nVideoView;
    Button driverButton;
    Button riderButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);


        nVideoView = (VideoView) findViewById(R.id.homepage_bgVideo);

        startService(new Intent(Homepage.this, onAppKilled.class));

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

        driverButton = (Button) findViewById(R.id.homepage_driver_button);
        driverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openLoginSignup();
            }
        });


        riderButton = (Button) findViewById(R.id.homepage_rider_button);
        riderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openLoginSignup2();
            }
        });


    }

    public void openLoginSignup() {

        Button button = findViewById(R.id.homepage_driver_button);
        String message = button.getText().toString();
        Intent intent = new Intent(Homepage.this, Login_Signup.class);
        intent.putExtra("Extra Message", message);
        startActivity(intent);
        finish();
    }

    public void openLoginSignup2() {
        Button button = findViewById(R.id.homepage_rider_button);
        String message = button.getText().toString();
        Intent intent = new Intent(Homepage.this, Login_Signup.class);
        intent.putExtra("Extra Message", message);
        startActivity(intent);
        finish();
    }

}
