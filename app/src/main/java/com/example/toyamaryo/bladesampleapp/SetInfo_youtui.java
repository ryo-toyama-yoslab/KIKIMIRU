package com.example.toyamaryo.bladesampleapp;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class SetInfo_youtui {
    private Activity mActivity;
    // Sound設定
    private SoundPlayer soundPlayer;
    private int nextInfoLevel;
    private boolean exitThread;

    public Handler handler;

    public SetInfo_youtui(Activity activity){
        mActivity = activity;
        soundPlayer = new SoundPlayer(mActivity);
        exitThread = false;
    }

    public void run(int nowLevel, Handler handler){
        this.handler = handler;

        Log.d("マルチスレッドに移行", "腰椎穿刺の注意喚起情報を提示するマルチスレッドに移行");
        if(nowLevel == 1){
            setInfo(1);
        }else if(nowLevel == 2){ //腰椎穿刺は設定したレベルが2以上ならアラートレベル3の情報のみを提示
            setInfo(3);
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
                        try {
                            setInfo(nextInfoLevel);
                        } catch (java.lang.NullPointerException e){
                            Log.d("腰椎穿刺マルチスレッド処理を中断", "処理中断のためにインスタンスをnullに変更，それに伴うエラー回避します");
                            e.printStackTrace();
                        }
                    }
                });
            }
        }).start();
    }

    private void setInfo(int level){
        if(level == 1) {
            Log.d("腰椎穿刺_レベル1-1", "腰椎穿刺レベル1の1つ目の情報を提示");

            //スピーカー鳴音
            soundPlayer.playLevel1Sound();

            //医療行為名を骨髄穿刺に変更
            ((TextView)mActivity.findViewById(R.id.iryou_name)).setText(R.string.iryo_name_YoutuiSensi);

            //アラートレベルが1であることを提示，テキストからを変更
            ((TextView)mActivity.findViewById(R.id.alert_level)).setText(R.string.alertLevel_one);
            ((TextView)mActivity.findViewById(R.id.alert_level)).setTextColor(mActivity.getResources().getColor(R.color.hud_yellow));

            //アラートレベル1の注意喚起情報を提示，テキストカラーを変更
            ((TextView)mActivity.findViewById(R.id.attention_info)).setTextColor(mActivity.getResources().getColor(R.color.hud_yellow));
            ((TextView)mActivity.findViewById(R.id.attention_info)).setText(mActivity.getResources().getString(R.string.spinal_level1_1));

            nextInfoLevel = 2; //次に提示する注意喚起情報のレベルを設定(アラートレベル1-2の情報に移るが仕様上2として設定)
            controlInfo();

        } else if (level == 2){
            Log.d("腰椎穿刺_レベル1-2", "腰椎穿刺レベル1の2つ目の情報を提示");

            //スピーカー鳴音
            soundPlayer.playLevel1Sound();

            //アラートレベルが1であることを提示，テキストからを変更
            ((TextView)mActivity.findViewById(R.id.alert_level)).setText(R.string.alertLevel_one);
            ((TextView)mActivity.findViewById(R.id.alert_level)).setTextColor(mActivity.getResources().getColor(R.color.hud_yellow));

            //アラートレベル1の注意喚起情報を提示，テキストカラーを変更
            ((TextView)mActivity.findViewById(R.id.attention_info)).setTextColor(mActivity.getResources().getColor(R.color.hud_yellow));
            ((TextView)mActivity.findViewById(R.id.attention_info)).setText(mActivity.getResources().getString(R.string.spinal_level1_2));

            nextInfoLevel = 3; //次に提示する注意喚起情報のレベルを設定
            controlInfo();

        }else if (level == 3){
            Log.d("腰椎穿刺_レベル3", "腰椎穿刺レベル3の情報を提示");

            //スピーカー鳴音
            soundPlayer.playLevel3Sound();

            //アラートレベルが3であることを提示，テキストからを変更
            ((TextView)mActivity.findViewById(R.id.alert_level)).setText(R.string.alertLevel_three);
            ((TextView)mActivity.findViewById(R.id.alert_level)).setTextColor(mActivity.getResources().getColor(R.color.hud_red));

            //アラートレベル3の注意喚起情報を提示，テキストカラーを変更
            ((TextView)mActivity.findViewById(R.id.attention_info)).setTextColor(mActivity.getResources().getColor(R.color.hud_red));
            ((TextView)mActivity.findViewById(R.id.attention_info)).setText(mActivity.getResources().getString(R.string.spinal_level3));
            exitThread = true;
            nextInfoLevel = 0; //次に提示する注意喚起情報のレベルを設定
            controlInfo();
        }else if (level == 0) {
            Log.d("腰椎穿刺_終了", "腰椎穿刺の情報を提示を終了");

            //アラートレベル表示を非表示
            mActivity.findViewById(R.id.alert_level).setVisibility(View.INVISIBLE);

            //アラートレベルの注意喚起情報を非表示
            mActivity.findViewById(R.id.attention_info).setVisibility(View.INVISIBLE);
            //全情報提示が終わったのでスレッドを終了
            stopThread();
        }
    }

    public void stopThread() {
        Log.d("腰椎穿刺の情報提示終了", "情報提示を中断もしくは終了します");
        soundPlayer = null;
        mActivity = null;
    }

}
