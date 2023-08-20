package com.example.toyamaryo.bladesampleapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

public class Setting extends Activity {

    public Switch s1, s2, s3;
    public Button setting_btn;
    int alertLevel = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        Log.d("設定画面を提示","アラートレベル設定画面を提示します");
        setting_btn = findViewById(R.id.setting_button);
        s1 = findViewById(R.id.alert_1_switch);
        s2 = findViewById(R.id.alert_2_switch);
        s3 = findViewById(R.id.alert_3_switch);

        setting_btn.setEnabled(false);//設定ボタンを非アクティブ
        s1.setText("未選択");
        s2.setText("未選択");
        s3.setText("未選択");

        Intent intentMain = getIntent();

        //alertLevel = Integer.parseInt(intentMain.getStringExtra("nowLevel"));
        Log.d("現在のアラートレベル",Integer.toString(intentMain.getIntExtra("nowLevel",0)));
        alertLevel = intentMain.getIntExtra("nowLevel",0);
        if(alertLevel == 1){
            s1.setChecked(true);
            s1.setText("選択");
            setting_btn.setEnabled(true);
        }else if(alertLevel == 2){
            s2.setChecked(true);
            s2.setText("選択");
            setting_btn.setEnabled(true);
        }else if(alertLevel == 3) {
            s3.setChecked(true);
            s3.setText("選択");
            setting_btn.setEnabled(true);
        }

        //未選択状態では設定ボタンを非アクティブ
        if(alertLevel == 0){
            setting_btn.setEnabled(false);
        }

        //レベル1(経験度低の医療従事者)を選択した場合
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
                    alertLevel = 1;
                    setting_btn.setEnabled(true);
                } else {
                    s1.setChecked(false);
                    s1.setText("未選択");
                    setting_btn.setEnabled(false);
                }
            }

        });

        //レベル2(経験度低の医療従事者)を選択した場合
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
                    alertLevel = 2;
                    setting_btn.setEnabled(true);
                }else{
                    s2.setChecked(false);
                    s2.setText("未選択");
                    setting_btn.setEnabled(false);
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
                    alertLevel = 3;
                    setting_btn.setEnabled(true);
                } else {
                    s3.setChecked(false);
                    s3.setText("未選択");
                    setting_btn.setEnabled(false);
                }
            }
        });

        //メイン画面(MainActivityに遷移するボタン)
        setting_btn.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   Intent intent = new Intent();
                   intent.putExtra("nowLevel",alertLevel);
                   setResult(RESULT_OK, intent);
                   Log.d("画面遷移","設定画面からメイン画面に遷移します．設定したレベルは：" + alertLevel);
                   finish();
               }
           }
        );

    }

}
