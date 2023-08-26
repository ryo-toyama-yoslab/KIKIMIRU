package com.example.toyamaryo.bladesampleapp;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class SetInfo_catheter {
    private Activity mActivity;
    // Sound設定
    private SoundPlayer soundPlayer;
    private int experimentMode;
    private double nextInfoPerLevel;
    private Handler handler;
    private boolean medNameSetOrNot; // 医療行為名が設定されているか
    private boolean exitThread; // システム終了検知フラグ(終了ボタンが押されたらfalseにして情報提示を中断)
    private boolean infoVisibility; // 情報提示オブジェクトの可視化フラグ

    SetInfo_catheter(Activity activity){
        mActivity = activity;
        soundPlayer = new SoundPlayer(mActivity);
        medNameSetOrNot = false;
        exitThread = false;
        infoVisibility = false;
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
        exitThread = true;
        Log.d("マルチスレッドに移行", "中心静脈カテーテル挿入の注意喚起情報を提示するマルチスレッドに移行");

        if(nowLevel == 1){
            setInfo(1, true);
        }else if(nowLevel >= 2){ // 腰椎穿刺は設定したレベルが2以上ならアラートレベル3の情報のみを提示
            setInfo(3, true);
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
                            if(exitThread) {
                                try {
                                    setInfo(nextInfoPerLevel, false);
                                }catch(java.lang.NullPointerException e) {
                                    Log.e("error","情報提示スレッド処理で縫null検知");
                                    e.printStackTrace();
                                }
                            }else{
                                Log.d("情報提示スレッド中断", "stopThread()が実行されました");
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void setInfo(double level, boolean firstSetInfo){
        if(level == 1) {
            Log.d("中心静脈カテーテル挿入_レベル1", "中心静脈カテーテル挿入レベル1の情報を提示");

            // 医療行為名設定
            if(!medNameSetOrNot) {
                setMedicalName();
                medNameSetOrNot = true;
            }

            //アラートレベルが1であることを提示，テキストからを変更
            ((TextView)mActivity.findViewById(R.id.alert_level)).setText(R.string.alertLevel_one);
            ((TextView)mActivity.findViewById(R.id.alert_level)).setTextColor(mActivity.getResources().getColor(R.color.hud_yellow));

            //アラートレベル1の注意喚起情報を提示，テキストカラーを変更
            ((TextView)mActivity.findViewById(R.id.attention_info)).setTextColor(mActivity.getResources().getColor(R.color.hud_yellow));
            ((TextView)mActivity.findViewById(R.id.attention_info)).setText(mActivity.getResources().getString(R.string.central_catheter_in_level1));

            if(!infoVisibility){ // 情報提示オブジェクト表示
                changeInfoVisible();
            }

            nextInfoPerLevel = 3; //次に提示する注意喚起情報のレベルを設定
            controlInfo();

        } else if (level == 3){
            Log.d("中心静脈カテーテル挿入_レベル3", "中心静脈カテーテル挿入レベル3の情報を提示");

            ///スピーカー鳴音
            if(!firstSetInfo){ // 2個目以降の情報提示
                if (experimentMode == 1) {
                    soundPlayer.playMechanicalSound();
                } else if (experimentMode == 2) {
                    soundPlayer.playCorrectVoiceSound();
                }
            }

            // 医療行為名設定
            if(!medNameSetOrNot) {
                setMedicalName();
                medNameSetOrNot = true;
            }

            //アラートレベルが3であることを提示，テキストからを変更
            ((TextView)mActivity.findViewById(R.id.alert_level)).setText(R.string.alertLevel_three);
            ((TextView)mActivity.findViewById(R.id.alert_level)).setTextColor(mActivity.getResources().getColor(R.color.hud_red));

            //アラートレベル3の注意喚起情報を提示，テキストカラーを変更
            ((TextView)mActivity.findViewById(R.id.attention_info)).setTextColor(mActivity.getResources().getColor(R.color.hud_red));
            ((TextView)mActivity.findViewById(R.id.attention_info)).setText(mActivity.getResources().getString(R.string.central_catheter_in_level3));

            if(!infoVisibility){ // 情報提示オブジェクト表示
                changeInfoVisible();
            }

            nextInfoPerLevel = 0; //次に提示する注意喚起情報のレベルを設定
            controlInfo();
        }else if (level == 0) {
            Log.d("中心静脈カテーテル挿入_終了", "中心静脈カテーテル挿入の情報を提示を終了");

            changeInfoInvisible(); // 情報提示オブジェクト非表示

            //全情報提示が終わったのでスレッドを終了
            stopThread();
        }
    }

    public boolean stopThread() {
        Log.d("中心静脈カテーテル挿入の情報提示終了", "情報提示を中断もしくは終了します");
        boolean checkStop;
        try {
            soundPlayer = null;
            mActivity = null;
            exitThread = false;
            checkStop = true;
        }catch(Exception e){
            checkStop = false;
            Log.e("InfoThreadStopError", e.toString());
        }

        return checkStop;
    }

    private void setMedicalName(){
        //医療行為名を骨髄穿刺に変更
        ((TextView)mActivity.findViewById(R.id.iryou_name)).setText(R.string.iryo_name_CatheterSounyuu);
    }

    private void changeInfoVisible(){
        mActivity.findViewById(R.id.iryou_name).setVisibility(View.VISIBLE); // 医療行為名を表示
        mActivity.findViewById(R.id.alert_level).setVisibility(View.VISIBLE); // アラートレベルを表示
        mActivity.findViewById(R.id.attention_info).setVisibility(View.VISIBLE); // 注意喚起情報を表示
    }

    private void changeInfoInvisible(){
        mActivity.findViewById(R.id.alert_level).setVisibility(View.INVISIBLE); // アラートレベル表示を非表示
        mActivity.findViewById(R.id.attention_info).setVisibility(View.INVISIBLE); // 注意喚起情報を非表示
    }

}
