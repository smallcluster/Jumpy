package com.smallcluster.jumpy.jeu;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.smallcluster.jumpy.R;

public class BMPManager {
    public final Bitmap SOL, CIEL, NUAGE, COLLINES, TETE, CORPS, BARRIERE, ANANAS;
    public BMPManager(Context c){
        Resources res = c.getResources();
        SOL = BitmapFactory.decodeResource(res, R.drawable.sol);
        CIEL = BitmapFactory.decodeResource(res, R.drawable.ciel);
        NUAGE = BitmapFactory.decodeResource(res, R.drawable.nuage);
        COLLINES = BitmapFactory.decodeResource(res, R.drawable.collines);
        TETE = BitmapFactory.decodeResource(res, R.drawable.face);
        CORPS = BitmapFactory.decodeResource(res, R.drawable.corps_joueur);
        BARRIERE = BitmapFactory.decodeResource(res, R.drawable.barriere);
        ANANAS = BitmapFactory.decodeResource(res, R.drawable.ananas);
    }
}
