package com.smallcluster.jumpy.jeu;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;

public class Joueur extends Sprite {
    private SFXManager sfxManager;
    private Bitmap textureTete;
    private final ObstacleManager obstacleManager;
    private float vx = 0, vy = 0;
    private int vie = 3;
    private boolean degatsSubit = false;
    private boolean surLeSol = false;
    private boolean demandeSaut = false;


    public Joueur(float x, float y, SFXManager sfxManager, BMPManager bmpManager, ObstacleManager obstacleManager) {
        super(x, y, bmpManager.CORPS);
        textureTete = bmpManager.TETE;
        this.sfxManager = sfxManager;
        this.obstacleManager = obstacleManager;
    }

    public void setTextureTete(Bitmap tete) {
        textureTete = tete;
    }

    public void setSfxManager(SFXManager sfxManager) {
        this.sfxManager = sfxManager;
    }

    public float getVx() {
        return vx;
    }

    public void setVx(float vx) {
        this.vx = vx;
    }

    public float getVy() {
        return vy;
    }

    public void setVy(float vy) {
        this.vy = vy;
    }

    public int getVie() {
        return vie;
    }

    public void setVie(int vie) {
        this.vie = vie;
    }

    public void actualiser(float delta, float accelx) {


        // L'utilisateur souhaite faire sauter le joueur
        if (!estMort() && demandeSaut) {
            sfxManager.play(sfxManager.SAUT);
            vy -= 200000 * delta;
            surLeSol = false;
            demandeSaut = false;
        }

        // Gravité
        if (!surLeSol) {
            // retombée plus rapide
            float grav = 12000;
            vy += grav * delta;
        }
        // Intégration implicite d'Euler
        y += vy * delta;
        x += vx * delta;

        if (!estMort()) {
            // Accéléromètre -> impulsion et non accélération
            if (surLeSol)
                x += accelx * delta;
            else
                x += 2 * accelx * delta; // plus de contrôles dans les airs
        }

        // Contact avec le sol
        float solPos = 590;
        if (y + height / 2.0f >= solPos && !surLeSol) {
            y = solPos - height / 2;
            vy = 0;
            surLeSol = true;
            sfxManager.play(sfxManager.TOMBE);
        }

        // Contact avec les bords de l'écran
        if (x - width / 2.0f <= 0) {
            x = width / 2.0f;
            vx = 0;
        } else if (x + width / 2.0f >= 1280) {
            x = 1280 - width / 2.0f;
            vx = 0;
        }

        // collision avec les obstacles -> perte d'une vie
        // effectif seulement si le timer d'invincibilité est terminé
        if (!estMort() && !degatsSubit && obstacleManager.collision(this)) {
            perdreVie();
        }
    }

    public void perdreVie() {
        vie--;
        degatsSubit = true;
        if (estMort()) {
            sfxManager.play(sfxManager.MEURT);

            // On change la hitbox
            float tmp = width;

            width = height;
            height = tmp;
            surLeSol = false;

            return;
        }
        sfxManager.play(sfxManager.BLESSE);
        Thread timer = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            degatsSubit = false;
        });
        timer.setDaemon(true);
        timer.start();
    }

    public boolean estMort() {
        return vie <= 0;
    }

    public void sauter() {
        demandeSaut = surLeSol;
    }

    @Override
    public void dessiner(Canvas c) {
        Paint paint = new Paint();
        if (degatsSubit) {
            ColorFilter filter = new LightingColorFilter(Color.RED, 0);
            paint.setColorFilter(filter);
        }
        if (estMort()) {
            super.dessiner(c, -90);
            matrix.reset();
            matrix.postTranslate(-textureTete.getWidth()/2.0f, -textureTete.getHeight()/2.0f);
            matrix.postRotate(-90);
            matrix.postTranslate(x- textureTete.getHeight()/2.0f - getHeight() / 2.0f, y);
            c.drawBitmap(textureTete, matrix, paint);
        } else {
            super.dessiner(c);
            c.drawBitmap(textureTete, x - textureTete.getWidth() / 2.0f, y - textureTete.getHeight() - height / 2.0f, paint);
        }
    }
}
