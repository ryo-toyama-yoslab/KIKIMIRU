package com.example.toyamaryo.bladesampleapp;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

public class SoundPlayer {

    private SoundPool soundPool;

    private int notice_sound;
    private int va_display_sound;
    private int va_correct_sound;
    private int va_checking_sound;
    private int va_explain_nextAction;

    public SoundPlayer(Context context) {

        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);

        // 音声データを読み込み
        notice_sound = soundPool.load(context, R.raw.level_1, 1);
        va_display_sound = soundPool.load(context, R.raw.va_display_info, 1);
        va_correct_sound = soundPool.load(context, R.raw.va_correct_info, 1);
        va_checking_sound = soundPool.load(context, R.raw.va_checking_info, 1);

        // 情報のレベルで通知音を分けてもレベルを直感的には理解しづらいので統一 ← 視覚情報として明示しているので問題なしと判断
        /*
        Level_1_Sound = soundPool.load(context, R.raw.level_1, 1);
        Level_2_Sound = soundPool.load(context, R.raw.level_2, 1);
        Level_3_Sound = soundPool.load(context, R.raw.level_3, 1);
        */
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
    public void playCorrectVoiceSound() {
        soundPool.play(va_correct_sound, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    // 音声の情報変更通知
    public void playCheckingVoiceSound() {
        soundPool.play(va_checking_sound, 1.0f, 1.0f, 1, 0, 1.0f);
    }
}