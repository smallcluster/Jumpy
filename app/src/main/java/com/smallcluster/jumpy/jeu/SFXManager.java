package com.smallcluster.jumpy.jeu;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import com.smallcluster.jumpy.R;

import java.util.ArrayList;

public class SFXManager {

    public final int SAUT, TOMBE, BLESSE, MEURT;

    private SoundPool soundPool;
    private final ArrayList<Integer> sounds = new ArrayList<>();
    public SFXManager(Context context){
        // Configure un soundPool (pour jouer plusieurs sfx en même temps)
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .setMaxStreams(8) // 8 sons possibles en même temps
                .build();
        SAUT = soundPool.load(context, R.raw.saut, 1);
        TOMBE = soundPool.load(context, R.raw.tombe, 1);
        BLESSE = soundPool.load(context, R.raw.blesse, 1);
        MEURT = soundPool.load(context, R.raw.meurt, 1);
        sounds.add(SAUT);
        sounds.add(TOMBE);
        sounds.add(BLESSE);
        sounds.add(MEURT);
    }

    public void play(int sfx){
        if(soundPool == null) return;
        soundPool.play(sfx, 1, 1, 1, 0, 1);
    }
    public void stop(int sfx){
        if(soundPool == null) return;
        soundPool.stop(sfx);
    }

    public void release(){
        for (int sfx: sounds) {
            soundPool.stop(sfx);
            soundPool.unload(sfx);
        }
        soundPool.release();
        soundPool = null;
    }

}
