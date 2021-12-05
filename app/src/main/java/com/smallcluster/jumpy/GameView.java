package com.smallcluster.jumpy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/*
Le jeu est est créé pour fonctionner sur une zone de 1280x720 pixels,
l'affichage est redimensionné en conservant le ratio.
 */

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    // contient les bordures de la surface
    private SurfaceHolder holder = null;
    // thread du jeu
    private Thread updateThread;
    // indique si l'on peut dessiner sur la surface
    private boolean surfaceReady = false;
    // test du thread
    private boolean updating = false;

    // 60 fps max
    private static final int MAX_FRAME_TIME = (int) (1000.0 / 60.0);
    private float delta = MAX_FRAME_TIME / 1000.0f;


    // --------------- IMAGES ----------------------
    private Bitmap BG_BMP = BitmapFactory.decodeResource(getResources(), R.drawable.bg);

    // ------------------------ MONDE --------------

    private float vitesse = 250; // en pixels/s
    private float decalageBG = 0;


    public void init(){
        setFocusable(true);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
    }

    public void render(Canvas c){
        Paint paint = new Paint();


        c.drawBitmap(BG_BMP, -decalageBG, 0, paint);
        c.drawBitmap(BG_BMP, -decalageBG+BG_BMP.getWidth(), 0, paint);

        decalageBG += vitesse*delta;
        if(decalageBG > c.getWidth())
            decalageBG = 0;


        paint.setColor(Color.RED);
        c.drawText("fps: "+(int) (1.0f/delta), 32, 32, paint);
    }

    public void update(){

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
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    public GameView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
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
            canvas = holder.lockHardwareCanvas(); // canvas GPU
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
                } catch (InterruptedException e) { }
            }
            delta = (System.nanoTime() - frameStartTimeNano) / 1000000000.0f;
        }
    }
}
