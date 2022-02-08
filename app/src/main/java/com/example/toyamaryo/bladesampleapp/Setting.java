package com.example.toyamaryo.bladesampleapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Switch;

import com.vuzix.hud.actionmenu.ActionMenuActivity;

public class Setting extends ActionMenuActivity {

    public Switch s1, s2, s3;
    int alertLevel = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        s1 = findViewById(R.id.alert_1_switch);
        s2 = findViewById(R.id.alert_2_switch);
        s3 = findViewById(R.id.alert_3_switch);
        s1.setText("未選択");
        s2.setText("未選択");
        s3.setText("未選択");

        Intent intentMain = getIntent();

        //alertLevel = Integer.parseInt(intentMain.getStringExtra("nowLevel"));
        Log.d("返ってきたアラートレベル",Integer.toString(intentMain.getIntExtra("nowLevel",0)));
        alertLevel = intentMain.getIntExtra("nowLevel",0);
        if(alertLevel == 1){
            s1.setChecked(true);
            s1.setText("選択");
        }else if(alertLevel == 2){
            s2.setChecked(true);
            s2.setText("選択");
        }else if(alertLevel == 3){
            s3.setChecked(true);
            s3.setText("選択");
        }


        //レベル1(経験度低の医療従事者)を選択した場合
        s1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if(s2.isChecked() == true || s3.isChecked()==true) {
                        s2.setChecked(false);
                        s2.setText("未選択");
                        s3.setChecked(false);
                        s3.setText("未選択");
                    }
                    s1.setChecked(true);
                    s1.setText("選択");
                    alertLevel = 1;
                } else {
                    s1.setChecked(false);
                    s1.setText("未選択");
                }

            }

        });

        //レベル2(経験度低の医療従事者)を選択した場合
        s2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if(s1.isChecked() == true || s3.isChecked()==true) {
                        s1.setChecked(false);
                        s1.setText("未選択");
                        s3.setChecked(false);
                        s3.setText("未選択");
                    }
                    s2.setChecked(true);
                    s2.setText("選択");
                    alertLevel = 2;
                }else{
                    s2.setChecked(false);
                    s2.setText("未選択");
                }
            }
        });

        //レベル3(経験度低の医療従事者)を選択した場合
        s3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if(s1.isChecked() == true || s2.isChecked()==true) {
                        s1.setChecked(false);
                        s1.setText("未選択");
                        s2.setChecked(false);
                        s2.setText("未選択");
                    }
                    s3.setChecked(true);
                    s3.setText("選択");
                    alertLevel = 3;
                } else {
                    s3.setChecked(false);
                    s3.setText("未選択");
                }
            }
        });

        //メイン画面(MainActivityに戻るボタン)
        Button setting_btn = findViewById(R.id.setting_button);
        setting_btn.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   Intent intent = new Intent();
                   intent.putExtra("nowLevel",alertLevel);
                   setResult(RESULT_OK, intent);
                   finish();
               }
           }
        );

    }
}
