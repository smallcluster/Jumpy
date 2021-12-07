package com.smallcluster.jumpy.jeu;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.smallcluster.jumpy.GameView;

import java.util.ArrayList;
import java.util.Random;

public class ObstacleManager {

    private final ArrayList<Sprite> pool = new ArrayList<>();
    private final BMPManager bmpManager;
    private float obstacleDistance = 720;
    private final Random RANDOM = new Random();

    public ObstacleManager(BMPManager bmpManager){
        this.bmpManager = bmpManager;
    }

    public void actualiser(float delta, float vitesse){
        obstacleDistance -= vitesse*delta;

        // Ajout d'un obstacle
        if(obstacleDistance <= 0){
            obstacleDistance = 680 + RANDOM.nextFloat()*600; // distance min = 680n max = 1280
            // Barrière.
            if(RANDOM.nextInt(2) == 0)
                pool.add(new Sprite(1280+bmpManager.BARRIERE.getWidth()/2.0f, 348+bmpManager.BARRIERE.getHeight()/2.0f, bmpManager.BARRIERE));
            // Ananas
            else
                pool.add(new Sprite(1280+bmpManager.ANANAS.getWidth()/2.0f, 470+bmpManager.ANANAS.getHeight()/2.0f, bmpManager.ANANAS));
        }

        // Déplacement des obstacles
        pool.forEach(o->o.setX(o.getX()-vitesse*delta));

        // On retire les obstalces hors camera
        pool.removeIf(o->o.getX()+o.getWidth() /2.0f <= 0);
    }

    public boolean collision(Rectangle autre){
        for(Sprite s : pool) {
            if (s.collision(autre))
                return true;
        }
        return false;
    }

    public void dessiner(Canvas c){
        pool.forEach(o->o.dessiner(c));
    }

}
