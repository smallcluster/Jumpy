package com.smallcluster.jumpy;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;

/*
Le jeu est est créé pour fonctionner sur une zone de 1280x720 pixels,
l'affichage est redimensionné en conservant le ratio.
 */

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable, SensorEventListener {

    private Paint paint = new Paint();
    private static final Random RANDOM = new Random();

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
    private final Bitmap CIEL_BMP = BitmapFactory.decodeResource(getResources(), R.drawable.ciel);
    private final Bitmap COLLINES_BMP = BitmapFactory.decodeResource(getResources(), R.drawable.collines);
    private final Bitmap NUAGE_BMP = BitmapFactory.decodeResource(getResources(), R.drawable.nuage);
    private final Bitmap SOL_BMP = BitmapFactory.decodeResource(getResources(), R.drawable.sol);
    private final Bitmap BARRIERE_BMP = BitmapFactory.decodeResource(getResources(), R.drawable.barriere);
    private final Bitmap ANANAS_BMP = BitmapFactory.decodeResource(getResources(), R.drawable.ananas);

    // ------------------ AUDIO ------------------
    MediaPlayer musiqueFond;
    SoundPool sfx;
    int sfx_saut, sfx_tombe, sfx_blesse, sfx_meurt;


    // ------------------------ MONDE --------------

    private float vitesse = 250; // en pixels/s
    private float decalageSol = 0;
    private float decalageCollines = 0;
    private float decalageNuage = 0;
    private float solPos = 590;
    private float grav = 12000;
    private float accelx = 0;
    private Joueur joueur = new Joueur(138, 472);
    private ArrayList<Obstacle> obstacles = new ArrayList<>();
    private float score = 0;

    private float obstacleDistance = 720;

    public enum ObstacleType {
        BARRIERE,
        ANANAS
    }


    private void init(Context c){
        setFocusable(true);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
    }

    public void setAvatar(Bitmap avatar){
        joueur.setAvatar(avatar);
    }

    public void sauter(){
        if(joueur.surLeSol){
            joueur.vy -= 200000*delta;
            joueur.surLeSol = false;
            // peut ne pas être initialisé
            if(sfx != null){
                sfx.play(sfx_saut, 1, 1, 1, 0, 1.0f);
            }
        }
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


        // Affichage des obsctacles
        obstacles.forEach(b->b.render(c));

        // Joueur
        joueur.render(c);

        // nuage
        c.drawBitmap(NUAGE_BMP, 18-decalageNuage, 34, null);
        c.drawBitmap(NUAGE_BMP, 18-decalageNuage+1280, 34, null);


        // Affichage des fps
        paint.setColor(Color.RED);
        c.drawText("fps: "+(int) (1.0f/delta), 32, 32, paint);

        // Affichage du score
        c.drawText("score : "+score, 32, 64, paint);
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
        decalageNuage += vitesse*0.5f*delta;
        if(decalageNuage >= 1280)
            decalageNuage = 0;

        // --------------- Joueur ----------------

        // gravité
        if(!joueur.surLeSol){
            // retombée plus rapide
            joueur.vy += grav * delta;
        }
        // Intégration implicite d'Euler
        joueur.y += joueur.vy * delta;
        joueur.x += joueur.vx * delta;

        // Accéléromètre -> impulsion et non accélération
        if(joueur.surLeSol)
            joueur.x += accelx*delta;
        else
            joueur.x += 2*accelx*delta; // plus de contrôles dans les airs

        // Contact avec le sol
        if(joueur.y + joueur.h/2.0f >= solPos && !joueur.surLeSol){
            joueur.y = solPos - joueur.h/2;
            joueur.vy = 0;
            joueur.surLeSol = true;
            sfx.play(sfx_tombe, 1, 1, 1, 0, 1.0f);
        }

        // Contact avec les bords de l'écran
        if(joueur.x - joueur.w/2.0f <= 0){
            joueur.x = joueur.w/2.0f;
            joueur.vx = 0;
        } else if(joueur.x + joueur.w/2.0f >= 1280) {
            joueur.x = 1280-joueur.w/2.0f;
            joueur.vx = 0;
        }

        // -------------- OBSTACLES ------------------------

        // Ajout d'un obstacle
        obstacleDistance -= vitesse*delta;

        if(obstacleDistance <= 0){
            obstacleDistance = 680 + RANDOM.nextFloat()*600;
            ObstacleType type;

            if(RANDOM.nextInt(2) == 0)
                type = ObstacleType.BARRIERE;
            else
                type = ObstacleType.ANANAS;

            obstacles.add(new Obstacle(type));
        }


        // Actualisation du pool d'obstacles
        for(Obstacle b : obstacles){
            b.x -= vitesse*delta; // déplacement avec le décor

            // Collision avec le joueur -> le joueur perd une vie
            if(b.collision(joueur)){
                joueur.perdreVie();
            }

            // disparition de l'obstacle
            if(b.x+b.w/2 <= 0)
                b.marquerARetirer();

        }
        // On vide le pool d'obstacles
        obstacles.removeIf(Obstacle::aRetirer);


        score += (vitesse/100.0f)*delta;
        vitesse += 10*delta;
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
        musiqueFond.release();
        musiqueFond = null;


        // supprimer les sfx
        sfx.stop(sfx_saut);
        sfx.stop(sfx_tombe);
        sfx.unload(sfx_saut);
        sfx.unload(sfx_tombe);
        sfx.unload(sfx_blesse);
        sfx.unload(sfx_meurt);
        sfx.release();
        sfx = null;

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

            // ------------ SFX -------------------

            // Configure un soundPool (pour jouer plusieurs sfx en même temps)
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            sfx = new SoundPool.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setMaxStreams(8) // 8 sons possibles en même temps
                    .build();

            sfx_saut = sfx.load(getContext(), R.raw.saut, 1);
            sfx_tombe = sfx.load(getContext(), R.raw.tombe, 1);
            sfx_blesse = sfx.load(getContext(), R.raw.blesse, 1);
            sfx_meurt = sfx.load(getContext(), R.raw.meurt, 1);


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
    public void onAccuracyChanged(Sensor sensor, int i) {}

    class Joueur extends Rectangle{
        public float vx=0, vy=0;
        public boolean surLeSol = false;
        private Bitmap avatar = null;
        private Bitmap corps = BitmapFactory.decodeResource(getResources(), R.drawable.corps_joueur);
        private int vie = 3;

        private boolean invincible = false;


        public Joueur(float x, float y){
            super(x,y, 47, 100);
        }
        public void setAvatar(Bitmap avatar){
            this.avatar = avatar;
        }

        public boolean estMort(){
            return vie <= 0;
        }

        public void perdreVie(){
            if(invincible) return;
            vie--;
            if(vie <= 0){
                invincible = true;
                sfx.play(sfx_meurt, 1, 1, 1, 0, 1);
                return;
            }
            invincible = true; // on rend invincible le joueur
            sfx.play(sfx_blesse, 1, 1, 1, 0, 1);

            // Joueur invincible pendant 1 seconde
            Thread timer = new Thread(()->{
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                invincible = false;
            });
            timer.setDaemon(true);
            timer.start();
        }

        @Override
        public void render(Canvas c){
            Paint paint = new Paint();

            if(invincible){
                ColorFilter filter = new LightingColorFilter(Color.RED, 0);
                paint.setColorFilter(filter);
            }

            if(avatar == null){
                super.render(c);
            } else {
                c.drawBitmap(avatar, x-50, y-150, paint);
            }

            c.drawBitmap(corps, x-24.5f, y-50, paint);
        }
    }

    class Obstacle extends Rectangle {
        private Bitmap sprite;
        private boolean retirer = false;

        public boolean aRetirer(){
            return retirer;
        }
        public void marquerARetirer(){
            retirer = true;
        }
        public Obstacle(ObstacleType type){
            super(1280,0, 0, 0);
            if(type == ObstacleType.BARRIERE){
                x = 1280;
                y = 348;
                sprite = BARRIERE_BMP;
            } else if(type == ObstacleType.ANANAS) {
                x = 1280;
                y = 470;
                sprite = ANANAS_BMP;
            }
            w = sprite.getWidth();
            h = sprite.getHeight();
            x+=w/2.0f;
            y+=h/2.0f;
        }
        @Override
        public void render(Canvas c){
            c.drawBitmap(sprite, x-w/2, y-h/2, null);
        }
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


