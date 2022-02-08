package com.example.toyamaryo.bladesampleapp;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

public class SoundPlayer {

    private static SoundPool soundPool;
    private static int Lecvel_1_Sound;
    private static int Lecvel_2_Sound;
    private static int Lecvel_3_Sound;


    public SoundPlayer(Context context) {

        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);

        Lecvel_1_Sound = soundPool.load(context, R.raw.level_1, 1);
        Lecvel_2_Sound = soundPool.load(context, R.raw.level_2, 1);
        Lecvel_3_Sound = soundPool.load(context, R.raw.level_3, 1);
    }

    public void playLevel1Sound() {
        soundPool.play(Lecvel_1_Sound, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playLevel2Sound() {
        soundPool.play(Lecvel_2_Sound, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void playLevel3Sound() {
        soundPool.play(Lecvel_3_Sound, 1.0f, 1.0f, 1, 0, 1.0f);
    }
}