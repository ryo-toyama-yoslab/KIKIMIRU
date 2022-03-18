package com.example.toyamaryo.bladesampleapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;

import com.vuzix.hud.actionmenu.ActionMenuActivity;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.MenuItem;
import android.view.Menu;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;


import static android.content.ContentValues.TAG;
import static java.lang.Thread.sleep;
//import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
//import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class MainActivity extends ActionMenuActivity{
    private MenuItem HelloMenuItem;
    private MenuItem VuzixMenuItem;
    private MenuItem BladeMenuItem;
    private ImageView encoded_bitmap;

    private UploadTask uploadTask;
    private TextView iryo_name;
    private TextView alert_level;
    private TextView attention_info;
    private TextView situation_info;

    private EditText editText;
    private Size mPreviewSize;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mPreviewSession;
    private TextureView mTextureView;
    private Camera mCamera;
    private CameraPreview mPreview;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private Bitmap theImage;
    private Bitmap bitmap2;
    public int picture_count;
    // Sound
    private SoundPlayer soundPlayer;
    // サーバからの結果保存用HashMap
    public HashMap<String,Integer> return_result = new HashMap<>();
    //保存された結果の数
    public int list_len;
    //情報提示フラグ
    public int info_flag=0; //提示する情報を変更するフラグ：0なら変更,1以上なら再認識
    //現在提示している情報
    public String now_info;

    //仲介用phpのアドレス
     String url = "https://grapefruit.sys.wakayama-u.ac.jp/~toyama/sample.php";
     int nowLevel = 0;
     Timer timer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iryo_name = findViewById(R.id.iryou_name);
        alert_level = findViewById(R.id.alert_level);
        situation_info = findViewById(R.id.situation_info);
        attention_info = findViewById(R.id.attention_info);

        picture_count = 0;

        soundPlayer = new SoundPlayer(this);

        if(nowLevel == 1){
            attention_info.setTextColor(getResources().getColor(R.color.hud_yellow));
        }else if(nowLevel == 2){
            attention_info.setTextColor(Color.argb(20,255,69,0));
        }else if(nowLevel == 3){
            attention_info.setTextColor(getResources().getColor(R.color.hud_red));
        }

        // Create an instance of Camera
        mCamera = getCameraInstance();

        // カメラ映像のプレビュー作成(撮影に必要)
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        //開始時(nowLevel=0)で設定画面に遷移
        Intent intent = new Intent(getApplication(), Setting.class);
        intent.putExtra("nowLevel",nowLevel);
        startActivityForResult(intent,1001);

        //設定画面に遷移
        Button setting_btn = findViewById(R.id.setting_button);
        setting_btn.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   Log.d("postLevel",Integer.toString(nowLevel));
                   Intent intent = new Intent(getApplication(), Setting.class);
                   intent.putExtra("nowLevel",nowLevel);
                   startActivityForResult(intent,1001);
               }
           }
        );


        // Add a listener to the Capture button
        if(checkCameraHardware(this)==true){
            Log.d("カメラの確認", "カメラの存在確認できました！" );
            final Button captureButton = findViewById(R.id.button_capture);
            captureButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            take_picture();
                            captureButton.setVisibility(View.INVISIBLE);
                        }
                    }
            );
        }else if(checkCameraHardware(this)==false){
            Log.d("カメラの確認", "カメラの存在確認できません" );
        }

    }




    protected void onActivityResult( int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(resultCode == RESULT_OK && requestCode == 1001 && intent != null) {
            nowLevel = intent.getIntExtra("nowLevel",0);
        }
    }


    public void take_picture(){
        mCamera.takePicture(null, null, mPicture);
    }


    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            picture_count++;

            //Log.d("画像データ", data.toString());
            ByteArrayInputStream imageInput = new ByteArrayInputStream(data);
            theImage = BitmapFactory.decodeStream(imageInput);
            bitmap2 = Bitmap.createScaledBitmap(theImage, 416, 416, false);


            //写真撮影後，サーバにアップロード
            uploadTask = new UploadTask(MainActivity.this);
            uploadTask.setListener(createListener());
            uploadTask.execute(new Param(url, bitmap2));
        }
    };


    //認識結果が返ってくる
    private UploadTask.Listener createListener() {
        return new UploadTask.Listener() {
            @Override
            public void onSuccess(String result) {

                if(picture_count < 10){
                    Log.d("retake_check", "送信画像が10枚以下のため再撮影:" + String.valueOf(picture_count));
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    Log.d("retake_check", "現在時刻:" + timeStamp);
                    take_picture();
                }else{
                    Log.d("retake_check", "送信画像が10枚のため再撮影終了" + String.valueOf(picture_count));
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    Log.d("retake_check", "現在時刻:" + timeStamp);
                    picture_count = 0;
                    if(return_result.get(result) == null){
                        return_result.put(result,1);                         //初の認識結果なら１を追加
                    }else {
                        return_result.put(result, return_result.get(result) + 1);//既に追加されてる結果は＋1
                    }

                    Log.d("result", "認識結果:" + result);
                    Log.d("result", "認識結果数:" + return_result.size());
                    Log.d("return_result", "認識結果蓄積状況:" + return_result);

                    if(return_result.size() == 1){         //１回目の認識結果が来た時の処理
                        if(result.contains("no_results")){
                            Log.d("認識失敗", "認識失敗のため再度認識を行います");
                        }else{
                            if(return_result.get(result) == 1){ //no_results以外の結果が初めて出た場合
                                now_info = result;
                                setInfo(result);
                            }else{ //システム開始から同じ認識結果が2回出た場合
                                Log.d("情報変更無し", "提示中の情報と同じ結果のため変更なしと判断");
                            }
                        }
                    }



                    if(return_result.size() > 1){          //認識２回目以降の処理，提示する情報を選択
                        if(result.contains("no_results")){
                            Log.d("no_results", "認識結果がno_resultsのため情報修正無し");
                        }else{
                            for (String key : return_result.keySet()) {

                                if(!key.equals("no_results")){

                                    if(return_result.get(key) > return_result.get(result)){
                                        Log.d("情報変更無し", "認識結果より情報変更の必要なしと判断");
                                        break;
                                    }else if(return_result.get(key) < return_result.get(result)){ //HashMapの全要素から判断する必要があるためフラグ1(他の認識結果数が多い場合があるため)
                                        info_flag = 0;
                                    }else{
                                        info_flag ++;
                                    }

                                    if(info_flag >= 1){ //認識結果が別の結果と同回数となったため再認識
                                        attention_info.setTextColor(getResources().getColor(R.color.hud_white));
                                        attention_info.setText(getResources().getString(R.string.alertLevel_default));
                                        attention_info.setTextColor(getResources().getColor(R.color.hud_white));
                                        attention_info.setText("再認識中");
                                    }else{
                                        //最新の認識結果が最も多く認識された結果のため更新(既に提示されているならそのまま)
                                        if(result.contains(now_info)){
                                            Log.d("情報変更無し", "提示中の情報と同じ結果のため変更なしと判断");
                                        }else{
                                            Log.d("情報変更", "新しい結果が適切な情報と判断");
                                            now_info = result;
                                            setInfo(result);
                                        }

                                    }

                                }

                            }

                        }

                    }

                    take_picture();
                }



            }
        };
    }

    public void setInfo(String result){
        //レベル1に設定した場合
        if(nowLevel == 1){
            //骨髄穿刺針の注意喚起情報表示
            if(result.contains("kotuzui")){
                Log.d("骨髄穿刺_レベル1", "骨髄穿刺レベル1の情報を提示");
                situation_info.setVisibility(View.INVISIBLE);
                alert_level.setText(R.string.alertLevel_one);
                alert_level.setTextColor(getResources().getColor(R.color.hud_yellow));
                iryo_name.setText(R.string.iryo_name_Kotuzuisennsi);
                soundPlayer.playLevel1Sound();
                attention_info.setTextColor(getResources().getColor(R.color.hud_yellow));
                attention_info.setText(getResources().getString(R.string.mark_level1));

                new Thread(new Runnable() {
                    public void run() {
                        Log.d("マルチスレッドに移行", "骨髄穿刺レベル2を表示するマルチスレッドに移行");
                        try {
                            Log.d("骨髄穿刺_レベル2待機", "骨髄穿刺レベル2を表示まで5秒待機");
                            Thread.sleep(5000); // 5秒待つ
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        attention_info.post(new Runnable() {
                            public void run() {
                                Log.d("骨髄穿刺_レベル2", "骨髄穿刺レベル2の情報を提示");
                                soundPlayer.playLevel2Sound();
                                alert_level.setText(R.string.alertLevel_two);
                                alert_level.setTextColor(Color.rgb(255,165,0));
                                attention_info.setTextColor(Color.rgb(255,165,0));
                                attention_info.setText(getResources().getString(R.string.mark_level2));
                            }
                        });

                        new Thread(new Runnable() {
                            public void run() {
                                Log.d("マルチスレッドに移行", "骨髄穿刺レベル3を表示するマルチスレッドに移行");
                                try {
                                    Log.d("骨髄穿刺_レベル3待機", "骨髄穿刺レベル3を表示まで5秒待機");
                                    Thread.sleep(5000); // 5秒待つ
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                attention_info.post(new Runnable() {
                                    public void run() {
                                        Log.d("骨髄穿刺_レベル3", "骨髄穿刺レベル3の情報を提示");
                                        soundPlayer.playLevel3Sound();
                                        alert_level.setText(R.string.alertLevel_three);
                                        alert_level.setTextColor(getResources().getColor(R.color.hud_red));
                                        attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                                        attention_info.setText(getResources().getString(R.string.mark_level3));
                                    }
                                });
                            }
                        }).start();

                    }
                }).start();


                /*
                try{
                    Thread.sleep(5000);
                }catch(InterruptedException e){
                }
                Log.d("骨髄穿刺_レベル2", "骨髄穿刺レベル2の情報を提示");
                soundPlayer.playLevel2Sound();
                attention_info.setTextColor(Color.rgb(255,165,0));
                attention_info.setText(getResources().getString(R.string.mark_level2));
                try{
                    Thread.sleep(5000);
                }catch(InterruptedException e) {
                }
                Log.d("骨髄穿刺_レベル3", "骨髄穿刺レベル3の情報を提示");
                soundPlayer.playLevel3Sound();
                attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                attention_info.setText(getResources().getString(R.string.mark_level3));
                */
            }


            //腰椎穿刺針の注意喚起情報表示
            if(result.contains("spinal_needle")){
                situation_info.setVisibility(View.INVISIBLE);
                soundPlayer.playLevel1Sound();
                attention_info.setTextColor(getResources().getColor(R.color.hud_yellow));
                attention_info.setText(getResources().getString(R.string.spinal_level1_1));
                try{
                    Thread.sleep(5000);
                }catch(InterruptedException e) {
                }
                soundPlayer.playLevel1Sound();
                attention_info.setText(getResources().getString(R.string.spinal_level1_2));
                try{
                    Thread.sleep(5000);
                }catch(InterruptedException e) {
                }
                soundPlayer.playLevel3Sound();
                attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                attention_info.setText(getResources().getString(R.string.spinal_level3));
            }

            //中心静脈カテーテル挿入の注意喚起情報表示
            if(result.contains("central_venous_catheter") && result.contains("guide_wire")){
                situation_info.setVisibility(View.INVISIBLE);
                soundPlayer.playLevel1Sound();
                attention_info.setTextColor(getResources().getColor(R.color.hud_yellow));
                attention_info.setText(getResources().getString(R.string.central_catheter_in_level1));
                try{
                    Thread.sleep(50000);
                }catch(InterruptedException e) {
                }
                soundPlayer.playLevel3Sound();
                attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                attention_info.setText(getResources().getString(R.string.central_catheter_in_level3));
            }

            //血液培養ボトルの注意喚起情報表示
            if(result.contains("blood_cl_bottle_orange") && result.contains("blood_cl_bottle_blue")){
                situation_info.setVisibility(View.INVISIBLE);
                soundPlayer.playLevel3Sound();
                attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                attention_info.setText(getResources().getString(R.string.blood_cl_bottle_level3));
            }
        }

        //レベル2に設定した場合
        else if(nowLevel == 2){
            //骨髄穿刺針の注意喚起情報表示
            if(result.contains("dog")){
                situation_info.setVisibility(View.INVISIBLE);
                alert_level.setText(R.string.alertLevel_two);
                alert_level.setTextColor(Color.rgb(255,165,0));
                iryo_name.setText(R.string.iryo_name_Kotuzuisennsi);
                soundPlayer.playLevel2Sound();
                attention_info.setTextColor(Color.rgb(255,165,0));//(getResources().getColor(R.color.hud_blue));
                attention_info.setText(getResources().getString(R.string.mark_level2));
                        /*try{
                            Thread.sleep(5000);
                        }catch(InterruptedException e) {
                        }
                        soundPlayer.playLevel3Sound();
                        attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                        attention_info.setText(getResources().getString(R.string.mark_level3));*/
            }
            //腰椎穿刺針の注意喚起情報表示
            if(result.contains("spinal_needle")){
                situation_info.setVisibility(View.INVISIBLE);
                soundPlayer.playLevel3Sound();
                attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                attention_info.setText(getResources().getString(R.string.spinal_level3));
            }
            //中心静脈カテーテル挿入の注意喚起情報表示
            if(result.contains("central_venous_catheter") && result.contains("guide_wire")){
                situation_info.setVisibility(View.INVISIBLE);
                soundPlayer.playLevel3Sound();
                attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                attention_info.setText(getResources().getString(R.string.central_catheter_in_level3));
            }
            //血液培養ボトルの注意喚起情報表示
            if(result.contains("blood_cl_bottle_orange") && result.contains("blood_cl_bottle_blue")){
                situation_info.setVisibility(View.INVISIBLE);
                soundPlayer.playLevel3Sound();
                attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                attention_info.setText(getResources().getString(R.string.blood_cl_bottle_level3));
            }
        }

        //レベル3に設定した場合
        else if(nowLevel == 3){
            //骨髄穿刺針の注意喚起情報表示
            if(result.contains("dog")){
                situation_info.setVisibility(View.INVISIBLE);
                soundPlayer.playLevel3Sound();
                iryo_name.setText(R.string.iryo_name_Kotuzuisennsi);
                attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                attention_info.setText(getResources().getString(R.string.mark_level3));
            }
            //腰椎穿刺針の注意喚起情報表示
            if(result.contains("spinal_needle")){
                situation_info.setVisibility(View.INVISIBLE);
                soundPlayer.playLevel3Sound();
                attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                attention_info.setText(getResources().getString(R.string.spinal_level3));
            }
            //中心静脈カテーテル挿入の注意喚起情報表示
            if(result.contains("central_venous_catheter") && result.contains("guide_wire")){
                situation_info.setVisibility(View.INVISIBLE);
                soundPlayer.playLevel3Sound();
                attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                attention_info.setText(getResources().getString(R.string.central_catheter_in_level3));
            }
            //血液培養ボトルの注意喚起情報表示
            if(result.contains("blood_cl_bottle_orange") && result.contains("blood_cl_bottle_blue")){
                situation_info.setVisibility(View.INVISIBLE);
                soundPlayer.playLevel3Sound();
                attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                attention_info.setText(getResources().getString(R.string.blood_cl_bottle_level3));
            }
        }else{
            soundPlayer.playLevel2Sound();
            attention_info.setTextColor(getResources().getColor(R.color.hud_white));
            attention_info.setText("アラートレベルを設定してください");
        }
    }

    public int getPictureCount(){
        return picture_count;
    }


    @Override
    protected boolean onCreateActionMenu(Menu menu) {
        super.onCreateActionMenu(menu);
        return true;
    }

    @Override
    protected boolean alwaysShowActionMenu() {
        return false;
    }

    @Override
    protected void onDestroy() {
        uploadTask.setListener(null);
        super.onDestroy();
    }
}