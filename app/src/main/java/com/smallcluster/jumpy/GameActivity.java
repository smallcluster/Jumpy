package com.smallcluster.jumpy;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

public class GameActivity extends AppCompatActivity {

    private GameView game;
    private SensorManager sensorManager;
    private Sensor sensor;
    private GestureDetectorCompat gestureDetector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        game = findViewById(R.id.gameView);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gestureDetector = new GestureDetectorCompat(this, new MyGestureListener());

        // On recup le selfie et on le transmet au joueur de la game view
        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
            Bitmap avatar = (Bitmap) bundle.get("avatar");
            game.setAvatar(avatar);
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }


    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(game, sensor, SensorManager.SENSOR_DELAY_GAME);
    }

    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(game);
    }

    // classe interne pour capturer l'instance de la la variable "game"
    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
            // detection d'un mouvement vert le haut

            // Vecteur directeur du mouvement
            double dx = event2.getX()-event1.getX();
            double dy = event2.getY()-event1.getY();
            double d = Math.sqrt(dx*dx+dy*dy);
            // Vecteur directeur normalisé
            double nx = dx / d;
            double ny = dy / d;

            // pointe vers le haut (repère de l'écran) et dans un conne de 45°
            // ie |nx| < sqrt(2)/2
            if(ny < 0 && Math.abs(nx) <= 0.7071067812){
                // On indique au jeu que l'on souhaite sauter
                // GameActivity.this.game est l'instance de GameView de la classe parente
                GameActivity.this.game.sauter();
            }
            return true;
        }
    }

}





