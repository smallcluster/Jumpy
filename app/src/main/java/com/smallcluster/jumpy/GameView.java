package com.smallcluster.jumpy;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.smallcluster.jumpy.jeu.BMPManager;
import com.smallcluster.jumpy.jeu.Joueur;
import com.smallcluster.jumpy.jeu.ObstacleManager;
import com.smallcluster.jumpy.jeu.SFXManager;

/*
Le jeu est est créé pour fonctionner sur une zone de 1280x720 pixels,
l'affichage est redimensionné en conservant le ratio.
 */

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable, SensorEventListener {

    private final Paint paint = new Paint();

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

    private float vitesse = 250; // en pixels/s
    private float decalageSol = 0;
    private float decalageCollines = 0;
    private float decalageNuage = 0;
    private final float solPos = 590;
    private final float grav = 12000;
    private float accelx = 0;

    private MediaPlayer musiqueFond;
    private BMPManager bmpManager;
    private SFXManager sfxManager;
    private Joueur joueur;
    private ObstacleManager obstacleManager;

    private float score = 0;

    private void init(Context c){
        setFocusable(true);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        bmpManager = new BMPManager(c);
        sfxManager = new SFXManager(c);
        obstacleManager = new ObstacleManager(bmpManager);
        joueur = new Joueur(138, 472, sfxManager, bmpManager, obstacleManager);
    }

    public void sauter(){
        joueur.sauter();
    }

    public void setJoueurVisage(Bitmap tete){
        joueur.setTextureTete(tete);
    }

    public void dessiner(Canvas c){

        // ciel fixe
        c.drawBitmap(bmpManager.CIEL, 0, 0, null);

        // collines
        c.drawBitmap(bmpManager.COLLINES, 38-decalageCollines, 423, null);
        c.drawBitmap(bmpManager.COLLINES, 38-decalageCollines+1280, 423, null);

        // sol
        c.drawBitmap(bmpManager.SOL, -decalageSol, 581, null);
        c.drawBitmap(bmpManager.SOL, -decalageSol+1280, 581, null);


        // Affichage des obsctacles
        obstacleManager.dessiner(c);

        // Joueur
        joueur.dessiner(c);

        // nuage
        c.drawBitmap(bmpManager.NUAGE, 18-decalageNuage, 34, null);
        c.drawBitmap(bmpManager.NUAGE, 18-decalageNuage+1280, 34, null);


        // Affichage des fps
        paint.setColor(Color.RED);
        c.drawText("fps: "+(int) (1.0f/delta), 32, 32, paint);

        // Affichage du score
        c.drawText("score : "+score, 32, 64, paint);
    }

    public void actualiser(){
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
        decalageNuage += vitesse*0.5f*delta;
        if(decalageNuage >= 1280)
            decalageNuage = 0;

        // --------------- OBSTACLES ----------------
        obstacleManager.actualiser(delta, vitesse);

        // ---------- JOUEUR ----------

        joueur.actualiser(delta, accelx);

        score += (vitesse/100.0f)*delta;
        vitesse += 10*delta;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

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
        musiqueFond.release();
        musiqueFond = null;

        // coupe les sons
        sfxManager.release();
        sfxManager = null;


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

            // charge et lance la  musique
            musiqueFond = MediaPlayer.create(getContext(), R.raw.main_music_2);
            musiqueFond.setLooping(true);
            musiqueFond.start();

            // charge les sons
            sfxManager = new SFXManager(getContext());
            joueur.setSfxManager(sfxManager);


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
                        actualiser();
                        dessiner(canvas);
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
    public void onAccuracyChanged(Sensor sensor, int i) {}

}


