package com.smallcluster.jumpy;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/*
Le jeu est est créé pour fonctionner sur une zone de 1280x720 pixels,
l'affichage est redimensionné en conservant le ratio.
 */

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable, SensorEventListener {

    private Paint paint = new Paint();

    // contient les bordures de la surface
    private SurfaceHolder holder = null;
    // thread du jeu
    private Thread updateThread;
    // indique si l'on peut dessiner sur la surface
    private boolean surfaceReady = false;
    // test du thread
    private boolean updating = false;


    // 60 fps max
    private static final int MAX_FRAME_TIME = 16;
    private float delta = 0.016f;


    // --------------- IMAGES ----------------------
    private Bitmap CIEL_BMP = BitmapFactory.decodeResource(getResources(), R.drawable.ciel);
    private Bitmap COLLINES_BMP = BitmapFactory.decodeResource(getResources(), R.drawable.collines);
    private Bitmap NUAGE_BMP = BitmapFactory.decodeResource(getResources(), R.drawable.nuage);
    private Bitmap SOL_BMP = BitmapFactory.decodeResource(getResources(), R.drawable.sol);

    // ------------------ AUDIO ------------------
    MediaPlayer musiqueFond;


    // ------------------------ MONDE --------------

    private float vitesse = 250; // en pixels/s
    private float decalageSol = 0;
    private float decalageCollines = 0;
    private float decalageNuage = 0;
    private float solPos = 600;
    private float grav = 2000;
    private float accelx = 0;
    private Joueur joueur = new Joueur(138, 482);



    public void init(Context c){
        setFocusable(true);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
    }

    public void render(Canvas c){

        // ciel fixe
        c.drawBitmap(CIEL_BMP, 0, 0, null);

        // collines
        c.drawBitmap(COLLINES_BMP, 38-decalageCollines, 423, null);
        c.drawBitmap(COLLINES_BMP, 38-decalageCollines+1280, 423, null);

        // sol
        c.drawBitmap(SOL_BMP, -decalageSol, 581, null);
        c.drawBitmap(SOL_BMP, -decalageSol+1280, 581, null);


        // Joueur
        joueur.render(c);

        // nuage
        c.drawBitmap(NUAGE_BMP, 18-decalageNuage, 34, null);
        c.drawBitmap(NUAGE_BMP, 18-decalageNuage+1280, 34, null);

        paint.setColor(Color.RED);
        c.drawText("fps: "+(int) (1.0f/delta), 32, 32, paint);
    }

    public void update(){
        // --------- PARALLAX -------------
        // collines
        decalageCollines += vitesse*0.25f*delta;
        if(decalageCollines >= 1280)
            decalageCollines = 0;

        // sol
        decalageSol += vitesse*delta;
        if(decalageSol >= 1280)
            decalageSol = 0;

        // nuage
        decalageNuage += vitesse*1.5f*delta;
        if(decalageNuage >= 1280)
            decalageNuage = 0;

        // --------------- Joueur ----------------

        // gravité
        if(!joueur.surLeSol){
            joueur.vy += grav * delta;
        }

        // Accéléromètre
        joueur.x += accelx * delta;

        // Intégration implicite d'Euler
        joueur.y += joueur.vy * delta;
        joueur.x += joueur.vx * delta;

        // Contact avec le sol
        if(joueur.y + joueur.h/2.0f > solPos){
            joueur.y = solPos - joueur.h/2;
            joueur.vy = 0;
            joueur.surLeSol = true;
        }

        // Contact avec les bords de l'écran
        if(joueur.x - joueur.w/2.0f <= 0){
            joueur.x = joueur.w/2.0f;
            joueur.vx = 0;
        } else if(joueur.x + joueur.w/2.0f >= 1280) {
            joueur.x = 1280-joueur.w/2.0f;
            joueur.vx = 0;
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        if (width == 0 || height == 0){
            return;
        }
        // ajuster les tailles
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder){
        this.holder = holder;
        holder.setFixedSize(1280, 720); // buffer interne en 720p
        holder.setKeepScreenOn(true);
        if (updateThread != null){
            updating = false;
            try{
                updateThread.join();
            } catch (InterruptedException ignored){}
        }
        surfaceReady = true;
        startUpdateThread();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        // La surface est détruite, on arrête le thread
        stopUpdateThread();
        // and release the surface
        holder.getSurface().release();
        this.holder = null;
        surfaceReady = false;
    }

    public void stopUpdateThread(){
        if (updateThread == null){
            return;
        }
        // coupe la musique
        musiqueFond.stop();
        musiqueFond = null;

        // coupe le thread
        updating = false;
        while (true){
            try{
                updateThread.join(5000);
                break;
            } catch (Exception e) {
                Log.e("surface", "Impossible de join le thread");
            }
        }
        updateThread = null;


    }

    public void startUpdateThread(){
        if (surfaceReady && updateThread == null){

            // lance la  musique
            musiqueFond = MediaPlayer.create(getContext(), R.raw.main_music_2);
            musiqueFond.setLooping(true);
            musiqueFond.start();

            updateThread = new Thread(this, "Game thread");
            updating = true;
            updateThread.start();
        }
    }

    public GameView(Context context) {
        super(context);
        init(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    public GameView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    @Override
    public void run() {
        long frameStartTimeNano;
        long frameTime;

        while (updating) {
            if (holder == null) {
                return;
            }
            frameStartTimeNano = System.nanoTime();
            Canvas canvas = null;
            if(holder.getSurface().isValid()) {
                canvas = holder.lockHardwareCanvas();
            }
            if (canvas != null) {
                try {
                    synchronized (holder) {
                        update();
                        render(canvas);
                    }
                } finally {
                    holder.unlockCanvasAndPost(canvas);
                }
            }

            // Calcul du temps d'attente pour avoir 60 fps au maximum
            frameTime = (System.nanoTime() - frameStartTimeNano) / 1000000;

            if (frameTime < MAX_FRAME_TIME){
                try {
                    Thread.sleep(MAX_FRAME_TIME - frameTime);
                    delta = 0.016f;
                } catch (InterruptedException e) { }
            } else {
                delta = frameTime / 1000.0f;
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;
        int rotation = ((Activity) getContext()).getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                accelx = -event.values[0]*200.0f;
                break;
            case Surface.ROTATION_90:
                accelx = event.values[1]*200.0f;
                break;
            case Surface.ROTATION_180:
                accelx = event.values[0]*200.0f;
                break;
            case Surface.ROTATION_270:
                accelx = -event.values[1]*200.0f;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}

class Rectangle {
    public float x,y,w,h;
    public Rectangle(float x, float y, float w, float h){
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public boolean collision(Rectangle autre){
        return !(autre.x-autre.w/2 > x+w/2 || autre.x+autre.w/2 < x-w/2 ||
                 autre.y-autre.h/2 > y+h/2 || autre.y+ autre.h/2 < y-h/2);
    }
    public void render(Canvas c){
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(2);
        c.drawRect(x-w/2, y-h/2, x+w/2, y+h/2, paint);
    }
}

class Joueur extends Rectangle{
    public float vx=0, vy=0;
    public boolean surLeSol = false;
    public Joueur(float x, float y){
        super(x,y, 200, 200);
    }
    @Override
    public void render(Canvas c){
        super.render(c);
    }
}
