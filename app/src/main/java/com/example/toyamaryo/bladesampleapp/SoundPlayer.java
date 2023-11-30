package com.example.toyamaryo.bladesampleapp;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

public class SoundPlayer {

    private SoundPool soundPool;

    // 電子音通知
    private int notice_sound;

    /*
    Tweet
    va ; voice assistant
    reading : 読み上げ
    */

    // 音声通知
    private int va_display_sound; // 情報提示
    private int va_change_sound; // 情報修正
    private int va_checking_sound; // 情報再確認

    // 情報読み上げ音声
    private int va_kotuzui_reading_1;
    private int va_kotuzui_reading_2;
    private int va_kotuzui_reading_3;


    public SoundPlayer(Context context) {

        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);

        // 電子音通知
        notice_sound = soundPool.load(context, R.raw.level_1, 1);

        //てすと
        va_display_sound = soundPool.load(context, R.raw.reading_kotuzui_1_with_presound, 1);
        va_change_sound = soundPool.load(context, R.raw.reading_kotuzui_2_with_presound, 1);
        va_checking_sound = soundPool.load(context, R.raw.reading_kotuzui_2_with_presound, 1);

        // 骨髄穿刺の読み上げ音声(読み上げ開始の通知音付き)
        va_kotuzui_reading_1 = soundPool.load(context, R.raw.reading_kotuzui_1_with_presound, 1);
        va_kotuzui_reading_2 = soundPool.load(context, R.raw.reading_kotuzui_2_with_presound, 1);
        va_kotuzui_reading_3 = soundPool.load(context, R.raw.reading_kotuzui_2_with_presound, 1);

        //情報修正音声


        // 音声データを読み込み
//        notice_sound = soundPool.load(context, R.raw.level_1, 1);
//        va_display_sound = soundPool.load(context, R.raw.va_display_info, 1);
//        va_change_sound = soundPool.load(context, R.raw.va_correct_info, 1);
//        va_checking_sound = soundPool.load(context, R.raw.va_checking_info, 1);


        // 情報のレベルで通知音を分けてもレベルを直感的には理解しづらいので統一 ← 視覚情報として明示しているので問題なしと判断
//        Level_1_Sound = soundPool.load(context, R.raw.level_1, 1);
//        Level_2_Sound = soundPool.load(context, R.raw.level_2, 1);
//        Level_3_Sound = soundPool.load(context, R.raw.level_3, 1);

    }

    // 機械音
    public void playMechanicalSound() {
        soundPool.play(notice_sound, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    // 音声の情報提示通知
    public void playDisplayVoiceSound() {
        soundPool.play(va_display_sound, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    // 音声の情報変更通知
    public void playChangeVoiceSound() {
        soundPool.play(va_change_sound, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    // 音声の情報変更通知
    public void playCheckingVoiceSound() {
        soundPool.play(va_checking_sound, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    // 音声の情報変更通知
    public void playReadingKotuzuiInfoSound(double level) {
        if(level == 1) {
            soundPool.play(va_kotuzui_reading_1, 1.0f, 1.0f, 1, 0, 1.0f);
        }else if(level == 2){
            soundPool.play(va_kotuzui_reading_2, 1.0f, 1.0f, 1, 0, 1.0f);
        }else if(level == 3) {
            soundPool.play(va_kotuzui_reading_3, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

}