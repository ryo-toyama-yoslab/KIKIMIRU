package com.example.toyamaryo.bladesampleapp;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class SetInfo_blood {

    private Activity mActivity;
    // Sound設定
    private SoundPlayer soundPlayer;
    private int experimentMode;
    private double nextInfoPerLevel;

    public Handler handler;

    public SetInfo_blood(Activity activity){
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
        Log.d("マルチスレッドに移行", "血液培養の注意喚起情報を提示するマルチスレッドに移行");

        if(nowLevel >= 1){
            setInfo(3);
        }
    }

    //現状では血液培養の情報が1つだけのためcontrolInfo()は未使用
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
                                Log.d("血液培養マルチスレッド処理を中断", "処理中断のためインスタンスをnull化済み，それに伴うエラー回避します");
                                //e.printStackTrace();
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void setInfo(double level){
        if(level == 3) {
            Log.d("血液培養_レベル3", "血液培養レベル3の情報を提示");

            //医療行為名を骨髄穿刺に変更
            ((TextView) mActivity.findViewById(R.id.iryou_name)).setText(R.string.iryo_name_KetuekiBaiyou);

            //アラートレベルが3であることを提示，テキストからを変更
            ((TextView) mActivity.findViewById(R.id.alert_level)).setText(R.string.alertLevel_three);
            ((TextView) mActivity.findViewById(R.id.alert_level)).setTextColor(mActivity.getResources().getColor(R.color.hud_red));

            //アラートレベル3の注意喚起情報を提示，テキストカラーを変更
            ((TextView) mActivity.findViewById(R.id.attention_info)).setTextColor(mActivity.getResources().getColor(R.color.hud_red));
            ((TextView) mActivity.findViewById(R.id.attention_info)).setText(mActivity.getResources().getString(R.string.central_catheter_in_level3));

            mActivity.findViewById(R.id.iryou_name).setVisibility(View.VISIBLE); // 医療行為名を表示
            mActivity.findViewById(R.id.alert_level).setVisibility(View.VISIBLE); // アラートレベルを表示
            mActivity.findViewById(R.id.attention_info).setVisibility(View.VISIBLE); // 注意喚起情報を表示

            nextInfoPerLevel = 0; //次に提示する注意喚起情報のレベルを設定
            controlInfo();
        }else if (level == 0) {
            Log.d("血液培養_終了", "血液培養の情報を提示を終了");

            mActivity.findViewById(R.id.alert_level).setVisibility(View.INVISIBLE); // アラートレベル表示を非表示
            mActivity.findViewById(R.id.attention_info).setVisibility(View.INVISIBLE); // 注意喚起情報を非表示

            // 全情報提示が終わったのでスレッドを終了
            stopThread();
        }

    }

    public void stopThread() {
        Log.d("血液培養の情報提示終了", "情報提示を中断もしくは終了します");
        soundPlayer = null;
        mActivity = null;
    }

}
