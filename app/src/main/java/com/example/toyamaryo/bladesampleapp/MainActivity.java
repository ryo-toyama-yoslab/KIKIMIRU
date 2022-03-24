package com.example.toyamaryo.bladesampleapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;

import com.vuzix.hud.actionmenu.ActionMenuActivity;

import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;


public class MainActivity extends ActionMenuActivity{

    //private UploadTask uploadTask;
    public UploadTaskSSL uploadTask; //SSL認証サーバとの接続用
    public TextView iryo_name;
    public TextView alert_level;
    public TextView attention_info;
    public TextView situation_info;

    private UploadTask.Listener listener;
    private Camera mCamera;
    private CameraPreview mPreview;
    private TakePicture take_picture;
    private Bitmap theImage;
    private Bitmap bitmap2;

    public Handler handler;

    //仲介用phpのアドレス
    private String url = "https://grapefruit.sys.wakayama-u.ac.jp/~toyama/sample.php";
    //private String url = "http://almond.sys.wakayama-u.ac.jp/~toyama/sample.php";

    public int picture_count;

    // Sound設定
    public SoundPlayer soundPlayer;
    // サーバからの結果保存用HashMap
    public HashMap<String,Integer> return_result = new HashMap<>();
    //情報提示フラグ
    public int info_flag=0; //提示する情報を変更するフラグ：0なら変更,1以上なら再認識
    //現在提示している情報
    public String now_info;

    //骨髄穿刺の注意喚起情報を提示するクラスのインスタンス
    SetInfo_kotuzui kotuzui;
    //腰椎穿刺の注意喚起情報を提示するクラスのインスタンス
    SetInfo_youtui youtui;
    //中心静脈カテーテル挿入の注意喚起情報を提示するクラスのインスタンス
    SetInfo_catheter catheter;
    //血液培養の注意喚起情報を提示するクラスのインスタンス
    SetInfo_blood blood;

    int nowLevel = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iryo_name = findViewById(R.id.iryou_name);
        alert_level = findViewById(R.id.alert_level);
        situation_info = findViewById(R.id.situation_info);
        attention_info = findViewById(R.id.attention_info);

        // メイン(UI)スレッドでHandlerのインスタンスを生成する
        handler = new Handler();

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

        //写真をサーバに送る用
        //uploadTask = new UploadTask();
        uploadTask = new UploadTaskSSL();

        //写真撮影用クラスのインスタンス作成
        take_picture = new TakePicture(mCamera, mPicture);

        listener = createListener();

        // カメラ映像のプレビュー作成(撮影に必要)
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        //注意喚起情報変更用関数(骨髄穿刺)
        kotuzui = new SetInfo_kotuzui(MainActivity.this);
        //注意喚起情報変更用関数(腰椎穿刺)
        youtui = new SetInfo_youtui(MainActivity.this);
        //注意喚起情報変更用関数(中心静脈カテーテル挿入)
        catheter = new SetInfo_catheter(MainActivity.this);
        //注意喚起情報変更用関数(血液培養)
        blood = new SetInfo_blood(MainActivity.this);

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
        if(checkCameraHardware(this)){
            Log.d("カメラの確認", "カメラの存在確認できました！" );
            final Button captureButton = findViewById(R.id.button_capture);
            captureButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            take_picture.execute(picture_count);
                            situation_info.setText("Now Recognition");
                            captureButton.setVisibility(View.INVISIBLE);
                        }
                    }
            );
        }else if(!checkCameraHardware(this)){
            Log.d("カメラの確認", "カメラの存在確認できません" );
        }

    }

    public void setPictureCount(int num){
        this.picture_count = num;
    }

    protected void onActivityResult( int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(resultCode == RESULT_OK && requestCode == 1001 && intent != null) {
            nowLevel = intent.getIntExtra("nowLevel",0);
        }
    }

    /*
    public void take_picture(){
        mCamera.takePicture(null, null, mPicture);
    }
    */


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
            //uploadTask = new UploadTask();
            uploadTask = new UploadTaskSSL();
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
                    //写真撮影用クラスのインスタンス作成
                    take_picture = new TakePicture(mCamera, mPicture);
                    take_picture.execute(picture_count);
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
                    //写真撮影用クラスのインスタンス作成(撮影には毎回作り直す必要有り)
                    take_picture = new TakePicture(mCamera, mPicture);
                    take_picture.execute(picture_count);
                }

            }
        };
    }

    public void setInfo(String result){

        //骨髄穿刺が結果として返された場合
        if(result.contains("kotuzui")){
            //Now Recognition表示を不可視
            situation_info.setVisibility(View.INVISIBLE);
            kotuzui.run(nowLevel,handler);
        }

        //腰椎穿刺が結果として返された場合
        if(result.contains("youtui")){
            //Now Recognition表示を不可視
            situation_info.setVisibility(View.INVISIBLE);
            youtui.run(nowLevel,handler);
        }

        //中心静脈カテーテル挿入が結果として返された場合
        if(result.contains("catheter")){
            //Now Recognition表示を不可視
            situation_info.setVisibility(View.INVISIBLE);
            catheter.run(nowLevel,handler);
        }

        //血液培養ボトルが結果として返された場合
        if(result.contains("blood")){
            //Now Recognition表示を不可視
            situation_info.setVisibility(View.INVISIBLE);
            blood.run(nowLevel,handler);
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