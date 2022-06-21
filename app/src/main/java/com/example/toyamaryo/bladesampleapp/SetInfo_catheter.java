package com.example.toyamaryo.bladesampleapp;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

public class SetInfo_catheter {

    private Activity mActivity;
    // Sound設定
    private SoundPlayer soundPlayer;
    private int nextInfoLevel;

    public Handler handler;

    public SetInfo_catheter(Activity activity){
        mActivity = activity;
        soundPlayer = new SoundPlayer(mActivity);
    }

    public void run(int nowLevel, Handler handler){
        this.handler = handler;

        Log.d("マルチスレッドに移行", "中心静脈カテーテル挿入の注意喚起情報を提示するマルチスレッドに移行");
        if(nowLevel == 1){
            setInfo(1);
        }else if(nowLevel == 2){ //中心静脈カテーテル挿入は設定したレベルが2以上ならアラートレベル3の情報のみを提示
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
                        }catch (java.lang.NullPointerException e){
                            Log.d("中心静脈カテーテル挿入マルチスレッド処理を中断", "処理中断のためにインスタンスをnullに変更，それに伴うエラー回避します");
                            e.printStackTrace();
                        }
                    }
                });

            }
        }).start();
    }

    private void setInfo(int level){
        if(level == 1) {
            Log.d("中心静脈カテーテル挿入_レベル1", "中心静脈カテーテル挿入レベル1の情報を提示");

            //スピーカー鳴音
            soundPlayer.playLevel1Sound();

            //医療行為名を骨髄穿刺に変更
            ((TextView)mActivity.findViewById(R.id.iryou_name)).setText(R.string.iryo_name_CatheterSounyuu);

            //アラートレベルが1であることを提示，テキストからを変更
            ((TextView)mActivity.findViewById(R.id.alert_level)).setText(R.string.alertLevel_one);
            ((TextView)mActivity.findViewById(R.id.alert_level)).setTextColor(mActivity.getResources().getColor(R.color.hud_yellow));

            //アラートレベル1の注意喚起情報を提示，テキストカラーを変更
            ((TextView)mActivity.findViewById(R.id.attention_info)).setTextColor(mActivity.getResources().getColor(R.color.hud_yellow));
            ((TextView)mActivity.findViewById(R.id.attention_info)).setText(mActivity.getResources().getString(R.string.central_catheter_in_level1));

            nextInfoLevel = 3; //次に提示する注意喚起情報のレベルを設定(アラートレベル1-2の情報に移るが仕様上2として設定)
            controlInfo();

        } else if (level == 3){
            Log.d("中心静脈カテーテル挿入_レベル3", "中心静脈カテーテル挿入レベル3の情報を提示");

            //スピーカー鳴音
            soundPlayer.playLevel3Sound();

            //アラートレベルが3であることを提示，テキストからを変更
            ((TextView)mActivity.findViewById(R.id.alert_level)).setText(R.string.alertLevel_three);
            ((TextView)mActivity.findViewById(R.id.alert_level)).setTextColor(mActivity.getResources().getColor(R.color.hud_red));

            //アラートレベル3の注意喚起情報を提示，テキストカラーを変更
            ((TextView)mActivity.findViewById(R.id.attention_info)).setTextColor(mActivity.getResources().getColor(R.color.hud_red));
            ((TextView)mActivity.findViewById(R.id.attention_info)).setText(mActivity.getResources().getString(R.string.central_catheter_in_level3));
        }
    }

    public void stopThread() {
        Log.d("中心静脈カテーテル挿入の情報提示中断", "再認識開始により情報提示を中断");
        soundPlayer = null;
        mActivity = null;
    }

}
