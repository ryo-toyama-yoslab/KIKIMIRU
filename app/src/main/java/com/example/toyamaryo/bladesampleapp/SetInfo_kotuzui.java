package com.example.toyamaryo.bladesampleapp;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class SetInfo_kotuzui {

    private Activity mActivity;
    // Sound設定
    private SoundPlayer soundPlayer;
    private int experimentMode;
    private double nextInfoPerLevel;
    private boolean running;
    private Handler handler;

    //取得する日時のフォーマットを指定
    final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    SetInfo_kotuzui(Activity activity){
        mActivity = activity;
        soundPlayer = new SoundPlayer(mActivity);
    }

    public void run(int nowLevel, int experimentMode, Handler handler){
        /*
            nowLevel
             1 : 手技熟練度 低
             2 : 手技熟練度 中
             3 : 手技熟練度 高

            experimentMode
             1 : 機械音通知
             2 : 音声通知
        */
        this.experimentMode = experimentMode;
        this.handler = handler;
        Log.d("マルチスレッドに移行", "骨髄穿刺の注意喚起情報を提示するマルチスレッドに移行");
        if(nowLevel == 1){
            setInfo(1);
        }else if(nowLevel == 2){
            setInfo(2);
        }else if(nowLevel == 3){
            setInfo(3);
        }
    }

    private void controlInfo(){
        new Thread(new Runnable() {
            public void run() {
                try {
                    Log.d("注意喚起情報変更インターバル", "次の注意喚起情報提示まで5秒待機");
                    Thread.sleep(5000); // 5秒待機
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                setInfo(nextInfoPerLevel);
                            }catch (java.lang.NullPointerException e){
                                Log.d("骨髄穿刺マルチスレッド処理を中断", "処理中断のためにインスタンスをnullに変更，それに伴うエラー回避します");
                                //e.printStackTrace();
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    Log.d("情報提示スレッドエラー","骨髄穿刺のcontrolInfoでエラー発生" + e.toString());
                }
            }
        }).start();
    }

    private void setInfo(double level){
        if(level == 1) {
            Log.d("骨髄穿刺_レベル1", "骨髄穿刺レベル1の情報を提示");
            //日時を指定したフォーマットで取得
            /*
            final Date date = new Date(System.currentTimeMillis());
            Log.d("現在時刻", "CurrentTime : " + df.format(date));
            */

            //スピーカー鳴音
            if(experimentMode == 1){
                soundPlayer.playMechanicalSound();
            }else if(experimentMode == 2){ // 音声通知
                soundPlayer.playDisplayVoiceSound();
            }

            //医療行為名を骨髄穿刺に変更
            ((TextView)mActivity.findViewById(R.id.iryou_name)).setText(R.string.iryo_name_Kotuzuisennsi);

            //アラートレベルが1であることを提示，テキストからを変更
            ((TextView)mActivity.findViewById(R.id.alert_level)).setText(R.string.alertLevel_one);
            ((TextView)mActivity.findViewById(R.id.alert_level)).setTextColor(mActivity.getResources().getColor(R.color.hud_yellow));

            //アラートレベル1の注意喚起情報を提示，テキストカラーを変更
            ((TextView)mActivity.findViewById(R.id.attention_info)).setTextColor(mActivity.getResources().getColor(R.color.hud_yellow));
            ((TextView)mActivity.findViewById(R.id.attention_info)).setText(mActivity.getResources().getString(R.string.mark_level1));

            nextInfoPerLevel = 2; //次に提示する注意喚起情報のレベルを設定
            controlInfo();

        } else if (level == 2){
            Log.d("骨髄穿刺_レベル2", "骨髄穿刺レベル2の情報を提示");
            //日時を指定したフォーマットで取得
            /*
            final Date date = new Date(System.currentTimeMillis());
            Log.d("現在時刻", "CurrentTime : " + df.format(date));
            */

            //スピーカー鳴音
            soundPlayer.playMechanicalSound();

            //アラートレベルが2であることを提示，テキストからを変更
            ((TextView)mActivity.findViewById(R.id.alert_level)).setText(R.string.alertLevel_two);
            ((TextView)mActivity.findViewById(R.id.alert_level)).setTextColor(Color.rgb(255,165,0));

            //アラートレベル2の注意喚起情報を提示，テキストカラーを変更
            ((TextView)mActivity.findViewById(R.id.attention_info)).setTextColor(Color.rgb(255,165,0));
            ((TextView)mActivity.findViewById(R.id.attention_info)).setText(mActivity.getResources().getString(R.string.mark_level2));

            nextInfoPerLevel = 3; //次に提示する注意喚起情報のレベルを設定
            running = true;
            controlInfo();

        }else if (level == 3){
            Log.d("骨髄穿刺_レベル3", "骨髄穿刺レベル3の情報を提示");
            //日時を指定したフォーマットで取得
            /*
            final Date date = new Date(System.currentTimeMillis());
            Log.d("現在時刻", "CurrentTime : " + df.format(date));
            */

            //スピーカー鳴音
            soundPlayer.playMechanicalSound();

            //アラートレベルが3であることを提示，テキストからを変更
            ((TextView)mActivity.findViewById(R.id.alert_level)).setText(R.string.alertLevel_three);
            ((TextView)mActivity.findViewById(R.id.alert_level)).setTextColor(mActivity.getResources().getColor(R.color.hud_red));

            //アラートレベル3の注意喚起情報を提示，テキストカラーを変更
            ((TextView)mActivity.findViewById(R.id.attention_info)).setTextColor(mActivity.getResources().getColor(R.color.hud_red));
            ((TextView)mActivity.findViewById(R.id.attention_info)).setText(mActivity.getResources().getString(R.string.mark_level3));

            nextInfoPerLevel = 0; //情報提示終了フラグを設定
            running = true;
            controlInfo();
        }else if (level == 0) {
            Log.d("骨髄穿刺_終了", "骨髄穿刺の情報を提示を終了");

            //アラートレベル表示を非表示
            mActivity.findViewById(R.id.alert_level).setVisibility(View.INVISIBLE);

            //アラートレベルの注意喚起情報を非表示
            mActivity.findViewById(R.id.attention_info).setVisibility(View.INVISIBLE);
            running = false;
            //全情報提示が終わったのでスレッドを終了
            stopThread();
        }
    }

    public void stopThread() {
        Log.d("骨髄穿刺の情報提示終了", "情報提示を中断もしくは終了します");
        //日時を指定したフォーマットで取得
        final Date date = new Date(System.currentTimeMillis());
        Log.d("現在時刻", "CurrentTime : " + df.format(date));
        soundPlayer = null;
        mActivity = null;
    }

}
