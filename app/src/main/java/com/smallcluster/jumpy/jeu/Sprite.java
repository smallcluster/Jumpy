package com.smallcluster.jumpy.jeu;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

public class Sprite extends Rectangle {
    protected Bitmap texture;
    protected Matrix matrix = new Matrix();


    public Sprite(float x, float y, Bitmap texture){
        super(x,y,texture.getWidth(), texture.getHeight());
        this.texture = texture;
    }
    public void dessiner(Canvas c){
        c.drawBitmap(texture, x-width/2.0f, y-height/2.0f, null);
    }

    public void dessiner(Canvas c, float angle){
        matrix.reset();
        matrix.postTranslate(-texture.getWidth()/2.0f, -texture.getHeight()/2.0f);
        matrix.postRotate(angle);
        matrix.postTranslate(x, y);
        c.drawBitmap(texture, matrix, null);
    }
}
