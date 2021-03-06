package com.example.toyamaryo.bladesampleapp;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

public class SetInfo_blood {

    private Activity mActivity;
    // Sound設定
    private SoundPlayer soundPlayer;
    private int nextInfoLevel;

    public Handler handler;

    public SetInfo_blood(Activity activity){
        mActivity = activity;
        soundPlayer = new SoundPlayer(mActivity);
    }

    public void run(int nowLevel, Handler handler){
        this.handler = handler;

        Log.d("マルチスレッドに移行", "血液培養の注意喚起情報を提示するマルチスレッドに移行");
        setInfo();
    }

    //現状では血液培養の情報が1つだけのためcontrolInfo()は未使用
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
                            setInfo();
                        }catch (java.lang.NullPointerException e){
                            Log.d("血液培養マルチスレッド処理を中断", "処理中断のためにインスタンスをnullに変更，それに伴うエラー回避します");
                            e.printStackTrace();
                        }
                    }
                });

            }
        }).start();
    }

    private void setInfo(){
        Log.d("血液培養_レベル3", "血液培養レベル3の情報を提示");

        //スピーカー鳴音
        soundPlayer.playLevel3Sound();

        //医療行為名を骨髄穿刺に変更
        ((TextView)mActivity.findViewById(R.id.iryou_name)).setText(R.string.iryo_name_KetuekiBaiyou);

        //アラートレベルが3であることを提示，テキストからを変更
        ((TextView)mActivity.findViewById(R.id.alert_level)).setText(R.string.alertLevel_three);
        ((TextView)mActivity.findViewById(R.id.alert_level)).setTextColor(mActivity.getResources().getColor(R.color.hud_red));

        //アラートレベル3の注意喚起情報を提示，テキストカラーを変更
        ((TextView)mActivity.findViewById(R.id.attention_info)).setTextColor(mActivity.getResources().getColor(R.color.hud_red));
        ((TextView)mActivity.findViewById(R.id.attention_info)).setText(mActivity.getResources().getString(R.string.central_catheter_in_level3));
    }

    public void stopThread() {
        Log.d("血液培養の情報提示中断", "再認識開始により情報提示を中断");
        soundPlayer = null;
        mActivity = null;
    }

}
