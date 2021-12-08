package com.smallcluster.jumpy.jeu;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class FonduNoir {

    private boolean fin;

    private final float duree;
    private final boolean fermeture;

    private float alpha;

    private final Paint paint = new Paint();

    public FonduNoir(float duree, boolean fermeture){
        this.duree = duree;
        this.fermeture = fermeture;
        alpha = fermeture ? 0.0f : 1.0f;
    }

    public void actualiser(float delta){
        if(fin) return;
        if(fermeture){
            alpha += delta/duree;
            if(alpha >= 1){
                alpha = 1;
                fin = true;
            }
        } else {
            alpha -= delta/duree;
            if(alpha <= 0){
                alpha = 0;
                fin = true;
            }
        }
    }

    public boolean estFini(){
        return fin;
    }

    public void dessiner(Canvas c){
        if(fin && !fermeture) return;
        paint.setColor(Color.argb(alpha, 0, 0, 0));
        c.drawRect(0, 0, c.getWidth(), c.getHeight(), paint);
    }

}
