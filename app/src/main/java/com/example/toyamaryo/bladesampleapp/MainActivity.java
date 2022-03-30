package com.example.toyamaryo.bladesampleapp;

import android.app.Activity;
import android.app.Application;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observer;

public class MainActivity extends ActionMenuActivity implements AsyncTaskCallback{

    private UploadTaskReady uploadTaskReady;
    private UploadTask uploadTask;
    private StartRecognition startRecognition;
    //public UploadTaskSSL uploadTask; //SSL認証サーバとの接続用
    public TextView iryo_name;
    public TextView alert_level;
    public TextView attention_info;
    public TextView situation_info;

    private StartRecognition.Listener listener;
    private Camera mCamera;
    private CameraPreview mPreview;
    private TakePicture take_picture;
    private Bitmap theImage;
    private Bitmap bitmap2;
    private AsyncTaskCallback callback = this;


    public Handler handler;

    //仲介用phpのアドレス
    //private String url = "https://grapefruit.sys.wakayama-u.ac.jp/~toyama/sample.php";
    private String url_0 = "http://almond.sys.wakayama-u.ac.jp/~toyama/ready.php";
    private String url = "http://almond.sys.wakayama-u.ac.jp/~toyama/sample.php";
    private String url_recognition = "http://almond.sys.wakayama-u.ac.jp/~toyama/start_recognition.php";

    public int picture_count;
    public ArrayList <String> result_str = new ArrayList<>();

    // Sound設定
    public SoundPlayer soundPlayer;
    // サーバからの結果保存用HashMap
    public HashMap<String,Integer> return_result = new HashMap<>();
    //情報提示フラグ
    public int info_flag=0; //提示する情報を変更するフラグ : 1で最初の情報提示，2以上で再認識
    //現在提示している情報
    public String now_info = null;

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
    protected final void onCreate(Bundle savedInstanceState) {
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
        uploadTask = new UploadTask(callback);
        //uploadTask = new UploadTaskSSL();

        //サーバに一時保存されている画像(9枚以下の時)を削除
        uploadTaskReady = new UploadTaskReady();
        Log.d("サーバ内不要画像をクリーン", "サーバ内にある不要な画像データを削除" );
        uploadTaskReady.execute(new Param(url_0));

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
                            //写真撮影用クラスのインスタンス作成
                            take_picture = new TakePicture(mCamera, mPicture);
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

    @Override
    public void onTaskFinished() {

    }

    @Override
    public void onTaskCancelled() {
        Log.d("onTaskCancelled", "画像送信が中断されました" );
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
            picture_count ++;
            //Log.d("画像データ", data.toString());
            ByteArrayInputStream imageInput = new ByteArrayInputStream(data);
            theImage = BitmapFactory.decodeStream(imageInput);
            bitmap2 = Bitmap.createScaledBitmap(theImage, 416, 416, false);



            //写真撮影後，サーバにアップロード
            uploadTask = new UploadTask(callback);
            //uploadTask = new UploadTaskSSL(); //SSL接続用
            uploadTask.setListener(u_createListener());
            uploadTask.execute(new Param(url, bitmap2));

            Log.d("ディレクトリパス", "ディレクトリパスの蓄積状況" + result_str);
            Log.d("SystemCheck", "サーバへのアップロードを行いました");

        }
    };


    //サーバに画像を送った結果が返ってくる，10枚以上で認識先のディレクトリパスがくる
    private UploadTask.Listener u_createListener() {
        return new UploadTask.Listener() {
            @Override
            public void onSuccess(String path){
                Log.d("SystemCheck", "サーバへの画像送信状況 : " + path);
                if(!path.equals("null")){
                    Log.d("SystemCheck", "-----------------------pathがnullではない------------------------------" + path);
                    picture_count = 0;
                    result_str.add(path);

                    Log.d("認識開始", "蓄積されているディレクトリで認識を開始します");
                    Log.d("ディレクトリパス", "ディレクトリパスの蓄積状況" + result_str);
                    startRecognition = new StartRecognition();
                    startRecognition.setListener(s_createListener());
                    startRecognition.execute(new Param(url_recognition, result_str.get(0)));
                    result_str.remove(0);

                }

                //写真撮影用クラスのインスタンス作成
                take_picture = new TakePicture(mCamera, mPicture);
                take_picture.execute(picture_count);

            }
        };
    }

    private StartRecognition.Listener s_createListener() {
        return new StartRecognition.Listener() {
            @Override
            public void onSuccess(String result){

                if(!result.equals("null")){
                    //Log.d("retake_check", "送信画像が10枚のため再撮影終了" + String.valueOf(picture_count));
                    Log.d("receive_result", "認識結果を受信したため，提示する情報を判定");


                    if(return_result.get(result) == null){
                        //初の認識結果なら１を追加
                        return_result.put(result,1);
                    }else {
                        //既に追加されてる結果は＋1
                        return_result.put(result, return_result.get(result) + 1);
                    }

                    Log.d("result", "認識結果:" + result);
                    Log.d("result", "認識結果数:" + return_result.size());
                    Log.d("return_result", "認識結果蓄積状況:" + return_result);

                    //1回目の認識結果が来た時の処理
                    if(return_result.size() == 1){
                        if(result.contains("no_results")){
                            Log.d("認識失敗", "認識失敗のため再度認識を行います");
                        }else{
                            if(return_result.get(result) == 1) {
                                //no_results以外の結果が初めて出た場合
                                Log.d("情報提示1回目", "認識された結果の情報を提示します");
                                now_info = result;
                                setInfo(result);
                            }else{ //システム開始から同じ認識結果が2回出た場合
                                Log.d("情報変更無し", "提示中の情報と同じ結果のため変更なしと判断");
                            }
                        }
                    }

                    //認識2回目以降の処理，提示する情報を選択
                    if(return_result.size() > 1){
                        if(result.contains("no_results")){
                            Log.d("no_results", "認識結果がno_resultsのため情報修正無し");
                        }else if(return_result.size() == 2 && now_info == null){
                            //1~*回目の認識結果がno_resultsで，初めて別の認識結果が出た場合の処理
                            Log.d("情報提示1回目", "認識された結果の情報を提示します");
                            now_info = result;
                            setInfo(result);
                        }else{
                            //認識結果を降順にソート
                            List<Entry<String, Integer>> list = new ArrayList<>(return_result.entrySet());
                            Collections.sort(list, new Comparator<Entry<String, Integer>>() {
                                //compareを使用して値を比較する
                                public int compare(Entry<String, Integer> obj1, Entry<String, Integer> obj2)
                                {
                                    //降順
                                    return obj2.getValue().compareTo(obj1.getValue());
                                }
                            });

                            for(Entry<String, Integer> entry : list) {
                                if (!entry.getKey().equals("no_results") && !entry.getKey().equals(result)) {
                                    if (entry.getValue() > return_result.get(result)) {
                                        Log.d("情報変更無し", "認識結果より情報変更の必要なしと判断");
                                        break;
                                    } else if (entry.getValue() < return_result.get(result)) {
                                        //新しい結果以外の蓄積されている結果と比べて回数が多い場合(降順にソートしているので1番目との比較結果で判断)
                                        if(result.contains(now_info)){
                                            //最新の認識結果が最も多く認識された結果のため更新(既に提示されているならそのまま)
                                            Log.d("情報変更無し", "提示中の情報と同じ結果のため変更なしと判断");
                                            break;
                                        }else{
                                            Log.d("情報変更", "新しい結果が適切な情報と判断");
                                            now_info = result;
                                            setInfo(result);
                                            break;
                                        }
                                    } else {
                                        //新しい認識結果が現状最も多い結果と同回数
                                        Log.d("再認識", "提示している情報の信頼性が低下したため再認識に移行");
                                        iryo_name.setText(getResources().getString(R.string.iryo_name_default));
                                        alert_level.setTextColor(getResources().getColor(R.color.hud_white));
                                        alert_level.setText(getResources().getString(R.string.alertLevel_default));
                                        attention_info.setTextColor(getResources().getColor(R.color.hud_white));
                                        attention_info.setText("再認識中");
                                        if(now_info.equals("kotuzui")){
                                            //情報提示用マルチスレッドを中断
                                            kotuzui.stopThread();
                                            //骨髄穿刺の注意喚起情報を提示するクラスのインスタンス
                                            kotuzui = new SetInfo_kotuzui(MainActivity.this);
                                        }else if(now_info.equals("youtui")){
                                            //情報提示用マルチスレッドを中断
                                            youtui.stopThread();
                                            //腰椎穿刺の注意喚起情報を提示するクラスのインスタンス
                                            SetInfo_youtui youtui;
                                        }else if(now_info.equals("catheter")){
                                            //情報提示用マルチスレッドを中断
                                            catheter.stopThread();
                                            //中心静脈カテーテル挿入の注意喚起情報を提示するクラスのインスタンス
                                            SetInfo_catheter catheter;
                                        }else if(now_info.equals("blood")){
                                            //情報提示用マルチスレッドを中断
                                            blood.stopThread();
                                            //血液培養の注意喚起情報を提示するクラスのインスタンス
                                            SetInfo_blood blood;
                                        }
                                    }
                                }else{
                                    Log.d("情報変更無し", "認識結果より情報変更の必要なしと判断");
                                }
                            }

                        }

                    }

                }
                /*
                //写真撮影用クラスのインスタンス作成
                take_picture = new TakePicture(mCamera, mPicture);
                take_picture.execute(picture_count);
                */
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
        startRecognition.setListener(null);
        super.onDestroy();
    }
}