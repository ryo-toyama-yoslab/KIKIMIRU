package com.example.toyamaryo.bladesampleapp;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;



public class SetInfo_kotuzui {

    private Activity mActivity;
    // Sound設定
    private SoundPlayer soundPlayer;
    private int nextInfoLevel;

    public Handler handler;

    public SetInfo_kotuzui(Activity activity){
        mActivity = activity;
        soundPlayer = new SoundPlayer(mActivity);
    }

    public void run(int nowLevel, Handler handler){
        this.handler = handler;

        Log.d("マルチスレッドに移行", "骨髄穿刺の注意喚起情報を提示するマルチスレッドに移行");
        Log.d("現在設定されているアラートレベル：","Level " + nowLevel);
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
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setInfo(nextInfoLevel);
                    }
                });

            }
        }).start();
    }

    private void setInfo(int level){
        if(level == 1) {
            Log.d("骨髄穿刺_レベル1", "骨髄穿刺レベル1の情報を提示");

            //スピーカー鳴音
           soundPlayer.playLevel1Sound();

            //医療行為名を骨髄穿刺に変更
            ((TextView)mActivity.findViewById(R.id.iryou_name)).setText(R.string.iryo_name_Kotuzuisennsi);

            //アラートレベルが1であることを提示，テキストからを変更
            ((TextView)mActivity.findViewById(R.id.alert_level)).setText(R.string.alertLevel_one);
            ((TextView)mActivity.findViewById(R.id.alert_level)).setTextColor(mActivity.getResources().getColor(R.color.hud_yellow));

            //アラートレベル1の注意喚起情報を提示，テキストカラーを変更
            ((TextView)mActivity.findViewById(R.id.attention_info)).setTextColor(mActivity.getResources().getColor(R.color.hud_yellow));
            ((TextView)mActivity.findViewById(R.id.attention_info)).setText(mActivity.getResources().getString(R.string.mark_level1));

            nextInfoLevel = 2; //次に提示する注意喚起情報のレベルを設定
            controlInfo();

        } else if (level == 2){
            Log.d("骨髄穿刺_レベル2", "骨髄穿刺レベル2の情報を提示");

            //スピーカー鳴音
            soundPlayer.playLevel2Sound();

            //アラートレベルが2であることを提示，テキストからを変更
            ((TextView)mActivity.findViewById(R.id.alert_level)).setText(R.string.alertLevel_two);
            ((TextView)mActivity.findViewById(R.id.alert_level)).setTextColor(Color.rgb(255,165,0));

            //アラートレベル2の注意喚起情報を提示，テキストカラーを変更
            ((TextView)mActivity.findViewById(R.id.attention_info)).setTextColor(Color.rgb(255,165,0));
            ((TextView)mActivity.findViewById(R.id.attention_info)).setText(mActivity.getResources().getString(R.string.mark_level2));

            nextInfoLevel = 3; //次に提示する注意喚起情報のレベルを設定
            controlInfo();

        }else if (level == 3){
            Log.d("骨髄穿刺_レベル3", "骨髄穿刺レベル3の情報を提示");

            //スピーカー鳴音
            soundPlayer.playLevel3Sound();

            //アラートレベルが3であることを提示，テキストからを変更
            ((TextView)mActivity.findViewById(R.id.alert_level)).setText(R.string.alertLevel_three);
            ((TextView)mActivity.findViewById(R.id.alert_level)).setTextColor(mActivity.getResources().getColor(R.color.hud_red));

            //アラートレベル3の注意喚起情報を提示，テキストカラーを変更
            ((TextView)mActivity.findViewById(R.id.attention_info)).setTextColor(mActivity.getResources().getColor(R.color.hud_red));
            ((TextView)mActivity.findViewById(R.id.attention_info)).setText(mActivity.getResources().getString(R.string.mark_level3));
        }
    }

}
