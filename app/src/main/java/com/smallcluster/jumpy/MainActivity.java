package com.smallcluster.jumpy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer menuSound;
    private int pos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button jouer = findViewById(R.id.buttonPlay);
        jouer.setOnClickListener(e ->{
            Intent intent = new Intent(this, SkinActivity.class);
            startActivity(intent);
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        menuSound = MediaPlayer.create(getBaseContext(), R.raw.menu);
        menuSound.setLooping(true);
        menuSound.seekTo(pos);
        menuSound.start();

    }

    @Override
    protected void onPause() {
        super.onPause();
        menuSound.pause();
        pos = menuSound.getCurrentPosition();
        menuSound.release();
        menuSound = null;
    }
}