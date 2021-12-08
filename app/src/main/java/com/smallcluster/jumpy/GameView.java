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
import com.smallcluster.jumpy.jeu.FonduNoir;
import com.smallcluster.jumpy.jeu.Joueur;
import com.smallcluster.jumpy.jeu.ObstacleManager;
import com.smallcluster.jumpy.jeu.SFXManager;

/*
Le jeu est est créé pour fonctionner sur une zone de 1280x720 pixels,
l'affichage est redimensionné en conservant le ratio.
 */

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable, SensorEventListener {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final String scoreString = getContext().getString(R.string.score);
    private final String gameOverString = getContext().getString(R.string.gameOver);

    private Bitmap photo;
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
    private float accelx = 0;

    private MediaPlayer musiqueFond;
    private MediaPlayer musiqueGameover;


    private BMPManager bmpManager;
    private SFXManager sfxManager;
    private Joueur joueur;
    private ObstacleManager obstacleManager;

    // score
    private float score = 0;
    private float infoAlphaTime = 0;


    private FonduNoir fonduDebut;
    private FonduNoir fonduFin;


    private void init(Context c){
        setFocusable(true);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        bmpManager = new BMPManager(c);
        sfxManager = new SFXManager(c);
        obstacleManager = new ObstacleManager(bmpManager);
        joueur = new Joueur(138, 472, sfxManager, bmpManager, obstacleManager);
        fonduDebut = new FonduNoir(2, false);
        fonduFin = new FonduNoir(2, true);
    }

    public void sauter(){
        joueur.sauter();
    }

    public void setJoueurVisage(Bitmap tete){
        photo = tete;
        joueur.setTextureTete(photo);
    }

    private float linearLoop(float t){
        return  t < 0.5f ? 2*t : 1-2*t;
    }

    public void rejouer(){

        if(!fonduFin.estFini()) return;
        // On réinitialise le jeu
        obstacleManager = new ObstacleManager(bmpManager);
        joueur = new Joueur(138, 472, sfxManager, bmpManager, obstacleManager);
        joueur.setTextureTete(photo);
        fonduDebut = new FonduNoir(2, false);
        fonduFin = new FonduNoir(2, true);
        vitesse = 250; // en pixels/s
        decalageSol = 0;
        decalageCollines = 0;
        decalageNuage = 0;
        score = 0;
        musiqueGameover.pause();
        musiqueFond.start();
    }



    public void dessiner(Canvas c){

        // screen de fin
        if(fonduFin.estFini()){
            c.drawRGB(0,0,0);
            // Affichage du score
            paint.setColor(Color.RED);
            paint.setTextSize(72);
            String scoreText = scoreString+" "+ (int) score;
            float scoreH = paint.descent() + paint.ascent();
            float cx = c.getWidth()/2.0f - paint.measureText(scoreText)/2.0f;
            float cy = c.getHeight()/2.0f - scoreH / 2.0f;
            c.drawText(scoreText, cx, cy, paint);

            // Affichage info pour rejouer
            paint.setTextSize(45);
            paint.setColor(Color.argb(linearLoop(infoAlphaTime), 1, 1, 1));
            String info = gameOverString;
            float infoX = c.getWidth()/2.0f - paint.measureText(info)/2.0f;
            float infoY = c.getHeight()/2.0f - scoreH + 24 - (paint.descent() + paint.ascent()) / 2.0f;
            c.drawText(info, infoX, infoY, paint);
            return;
        }

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





        // Affichage des vies du joueur
        for(int i=0; i < joueur.getVie(); i++){
            c.drawBitmap(bmpManager.COEUR, i*bmpManager.COEUR.getWidth(), 0, null);
        }



        // Affichage du score
        paint.setColor(Color.RED);
        paint.setTextSize(64);
        String scoreText = scoreString+" "+ (int) score;
        float cx = c.getWidth()/2.0f - paint.measureText(scoreText)/2.0f;
        float cy =  22-(paint.descent() + paint.ascent());
        c.drawText(scoreText, cx, cy, paint);

        // Fondu en ouverture lors du lancement du jeu
        fonduDebut.dessiner(c);

        // Le joueur est mort -> fondu en fermeture
        if(joueur.estMort())
            fonduFin.dessiner(c);


    }

    public void actualiser(){

        // La partie est terminée.
        if(fonduFin.estFini()){
            // Gestion du timer du clignottement du texte.
            infoAlphaTime += delta;
            if(infoAlphaTime >= 1)
                infoAlphaTime = 0;
            return;
        }


        // Le joueur est mort -> fondu en fermeture
        if(joueur.estMort()){
            fonduFin.actualiser(delta);
            // On change de musique
            if(!musiqueGameover.isPlaying()){
                musiqueGameover.start();

                // Pause et remise au début
                musiqueFond.pause();
                musiqueFond.seekTo(0);
            }

        }

        // Fondu en ouverture lors du lancement du jeu
        fonduDebut.actualiser(delta);

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

        if(!joueur.estMort())
            score += delta*vitesse/100.0f;

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
        // charge et lance la  musique
        musiqueFond = MediaPlayer.create(getContext(), R.raw.main_music_2);
        musiqueFond.setLooping(true);

        if(!joueur.estMort())
            musiqueFond.start();

        musiqueGameover = MediaPlayer.create(getContext(), R.raw.gameover);
        musiqueGameover.setLooping(true);

        if(joueur.estMort())
            musiqueGameover.start();

        // charge les sons
        sfxManager = new SFXManager(getContext());
        joueur.setSfxManager(sfxManager);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        // La surface est détruite, on arrête le thread
        stopUpdateThread();
        // and release the surface
        holder.getSurface().release();
        this.holder = null;
        surfaceReady = false;

        // coupe la musique
        musiqueFond.stop();
        musiqueFond.release();
        musiqueFond = null;

        musiqueGameover.stop();
        musiqueGameover.release();
        musiqueGameover = null;

        // coupe les sons
        sfxManager.release();
        sfxManager = null;
    }

    public void stopUpdateThread(){
        if (updateThread == null){
            return;
        }

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


