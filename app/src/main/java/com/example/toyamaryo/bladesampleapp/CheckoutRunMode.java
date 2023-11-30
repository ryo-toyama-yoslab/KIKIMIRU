package com.example.toyamaryo.bladesampleapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

public class CheckoutRunMode extends Activity {

    public Switch s1, s2, s3;
    public Button checkout_btn;
    int mode = 0; // mode1 : 通知音無し, mode2 : 機械通知音, mode3 : 音声通知

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout_experiment);

        Log.d("モード設定画面を提示","モード設定画面を提示します");
        checkout_btn = findViewById(R.id.setting_button);
        s1 = findViewById(R.id.run_mode_1_switch);
        s2 = findViewById(R.id.run_mode_2_switch);
        s3 = findViewById(R.id.run_mode_3_switch);

        checkout_btn.setEnabled(false);//設定ボタンを非アクティブ
        s1.setText("未選択");
        s2.setText("未選択");
        s3.setText("未選択");

        Intent intentMain = getIntent();

        // 一番上のボタンにフォーカスしておく
        s1.requestFocus();

        Log.d("現在のモード",Integer.toString(intentMain.getIntExtra("experimentMode",0)));
        mode = intentMain.getIntExtra("experimentMode",0);
        if(mode == 1){
            s1.setChecked(true);
            s1.setText("選択");
            checkout_btn.setEnabled(true);
        }else if(mode == 2){
            s2.setChecked(true);
            s2.setText("選択");
            checkout_btn.setEnabled(true);
        }else if(mode == 3){
            s2.setChecked(true);
            s2.setText("選択");
            checkout_btn.setEnabled(true);
        }

        //未選択状態では設定ボタンを非アクティブ
        if(mode == 0){
            checkout_btn.setEnabled(false);
        }

        //mode①を選択した場合 機械音通知
        s1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if(s2.isChecked() || s3.isChecked()) {
                        s2.setChecked(false);
                        s2.setText("未選択");
                        s3.setChecked(false);
                        s3.setText("未選択");
                    }
                    s1.setChecked(true);
                    s1.setText("選択");
                    mode = 1;
                    checkout_btn.setEnabled(true);
                } else {
                    s1.setChecked(false);
                    s1.setText("未選択");
                    checkout_btn.setEnabled(false);
                }
            }

        });

        //mode②を選択した場合 音声通知
        s2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if(s1.isChecked() || s3.isChecked()) {
                        s1.setChecked(false);
                        s1.setText("未選択");
                        s3.setChecked(false);
                        s3.setText("未選択");
                    }
                    s2.setChecked(true);
                    s2.setText("選択");
                    mode = 2;
                    checkout_btn.setEnabled(true);
                }else{
                    s2.setChecked(false);
                    s2.setText("未選択");
                    checkout_btn.setEnabled(false);
                }
            }
        });

        //レベル3(経験度低の医療従事者)を選択した場合
        s3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if(s1.isChecked() || s2.isChecked()) {
                        s1.setChecked(false);
                        s1.setText("未選択");
                        s2.setChecked(false);
                        s2.setText("未選択");
                    }
                    s3.setChecked(true);
                    s3.setText("選択");
                    mode = 3;
                    checkout_btn.setEnabled(true);
                } else {
                    s3.setChecked(false);
                    s3.setText("未選択");
                    checkout_btn.setEnabled(false);
                }
            }
        });


        //メイン画面(MainActivityに遷移するボタン)
        checkout_btn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               Intent intent = new Intent();
               intent.putExtra("experimentMode",mode);
               setResult(RESULT_OK, intent);
               Log.d("画面遷移","実行モード変更画面から設定画面に遷移します．設定したモードは：" + mode);
               finish();
           }
       });
    }

}
