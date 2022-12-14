package com.example.toyamaryo.bladesampleapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;

import com.vuzix.hud.actionmenu.ActionMenuActivity;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

public class MainActivity extends ActionMenuActivity{

    //SSL認証サーバとの接続用
    private UploadTaskSSL uploadTaskSSL;
    private UploadTaskReadySSL uploadTaskReadySSL;

    //非SSL認証サーバとの接続用
    private UploadTask uploadTask;
    private UploadTaskReady uploadTaskReady;

    //認識結果確認用非同期処理クラスのインスタンス
    private GetResultTaskSSL getResultTaskSSL;
    private GetResultTask getResultTask;

    public TextView iryo_name;
    public TextView alert_level;
    public TextView attention_info;
    public ProgressBar progressBar;

    private Camera mCamera;
    private CameraPreview mPreview;
    private Bitmap theImage;
    private Button captureButton;

    public Handler mainHandler;
    public Handler shotHandler;
    public Runnable shotRun;
    public Handler getResultHandler;
    public Runnable getResultRun;
    public Handler saveLogHandler;

    private boolean ShotFlag = false;//撮影を開始しているか
    private boolean getRunnnig = false;//認識結果取得実行用フラグ
    private boolean displayInfoFlag = false;

    //仲介用phpのアドレス(grapefruitサーバ用，SSL)
    private String url_0 = "https://grapefruit.sys.wakayama-u.ac.jp/~toyama/ready.php";
    private String url = "https://grapefruit.sys.wakayama-u.ac.jp/~toyama/getImage.php";
    private String url_get = "https://grapefruit.sys.wakayama-u.ac.jp/~toyama/returnRecognitionResult.php";


    //仲介用phpのアドレス
    /*
    private String url_0 = "http://172.30.184.57/~toyama/ready.php";
    private String url = "http://172.30.184.57/~toyama/getImage.php";
    private String url_get = "http://172.30.184.57/~toyama/returnRecognitionResult.php";
    */

    private int nowLevel;

    // Sound設定
    public SoundPlayer soundPlayer;


    // サーバからの結果保存用HashMap
    public HashMap<String,Integer> return_result = new HashMap<>();


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


    //ログ保存用クラスのインスタンス
    SaveLog saveLog;

    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("StartSysytem_Log" ,"ReadLog");

        try{
            Runtime.getRuntime().exec(new String[] { "logcat", "-c"}); //以前のログをクリア
        }catch(Exception e){
            e.printStackTrace();
        }

        Log.d("LifeCycleCheck", "onCreate()が呼び出されました");

        setContentView(R.layout.activity_main);
        iryo_name = findViewById(R.id.iryou_name);
        alert_level = findViewById(R.id.alert_level);
        attention_info = findViewById(R.id.attention_info);
        //progressBar = findViewById(R.id.progressBar);

        // 情報提示中の中断通知用Handlerを生成
        mainHandler = new Handler();
        // 撮影スレッドの停止用Handlerを生成
        shotHandler = new Handler();
        // 結果取得用スレッドの停止用フラグを生成
        getResultHandler = new Handler();
        // ログ保存スレッドの停止用Handlerを生成
        saveLogHandler = new Handler();

        nowLevel = 0;

        soundPlayer = new SoundPlayer(this);

        // Create an instance of Camera
        mCamera = getCameraInstance();

        // カメラ映像のプレビュー作成(撮影に必要)
        mPreview = new CameraPreview(this, mCamera);

        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);


        // mPreviewのSurfaceHolder取得用
        //mHolder = mPreview.returnHolder();

        if(nowLevel == 1){
            attention_info.setTextColor(getResources().getColor(R.color.hud_yellow));
        }else if(nowLevel == 2){
            attention_info.setTextColor(Color.argb(20,255,69,0));
        }else if(nowLevel == 3){
            attention_info.setTextColor(getResources().getColor(R.color.hud_red));
        }

        //SSL用
        //サーバに一時保存されている画像(9枚以下の時)を削除

        uploadTaskReadySSL = new UploadTaskReadySSL();
        Log.d("サーバ内不要画像をクリーン", "サーバ内にある不要な画像データを削除" );
        uploadTaskReadySSL.execute(new Param(url_0));


        //非SSL用
        /*
        uploadTaskReady = new UploadTaskReady();
        Log.d("サーバ内不要画像をクリーン", "サーバ内にある不要な画像データを削除" );
        uploadTaskReady.execute(new Param(url_0));
        */

        captureButton = findViewById(R.id.button_capture);

        //注意喚起情報変更用インスタンス生成(骨髄穿刺)
        kotuzui = new SetInfo_kotuzui(MainActivity.this);
        //注意喚起情報変更用インスタンス生成(腰椎穿刺)
        youtui = new SetInfo_youtui(MainActivity.this);
        //注意喚起情報変更用インスタンス生成(中心静脈カテーテル挿入)
        catheter = new SetInfo_catheter(MainActivity.this);
        //注意喚起情報変更用インスタンス生成(血液培養)
        blood = new SetInfo_blood(MainActivity.this);

        //ログ保存用インスタンス生成
        saveLog = new SaveLog(this);

        //設定画面に遷移
        Button setting_btn = findViewById(R.id.setting_button);
        setting_btn.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   Log.d("postLevel",Integer.toString(nowLevel));
                   Intent intent = new Intent(getApplication(), Setting.class);
                   intent.putExtra("nowLevel",nowLevel);
                   Log.d("SystemCheck","startActivityを行う前です");
                   startActivityForResult(intent,1001);
                   Log.d("SystemCheck","startActivityを行った後です");
               }
           }
        );

        Button end_btn = findViewById(R.id.end_button);
        end_btn.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   Log.d("EndButton","終了ボタンが押されました");
                   try{
                       stopInfo();
                       Log.d("情報提示スレッド停止 : ","成功");
                   }catch(Exception e){
                       Log.d("情報提示スレッド停止エラー",e.toString());
                   }
                   moveTaskToBack(true); //アプリケーション全体を中断 onDestroy()が呼ばれなければonRestart()で再開
                   getRunnnig = false;
                   getResultTaskSSL.removeListener(g_createListenerSSL());
                   //getResultTask.removeListener(g_createListener());
                   try {
                       Thread.sleep(5000); // 5秒待機
                   }catch (Exception e){
                       Log.d("アプリ終了待機中エラー",e.toString());
                   }
                   finish(); //アプリケーション終了 再開時はonCreate()から(ログ送信のために5秒待機後に完全終了)
               }
            }
        );

        // Add a listener to the Capture button
        if(checkCameraHardware(this)){
            Log.d("カメラの確認", "カメラの存在確認できました！" );
            captureButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d("PushCameraButton", "カメラ撮影開始" );
                            ShotFlag = true;
                            //progressBar.setVisibility(View.VISIBLE);
                            iryo_name.setText(getResources().getString(R.string.iryo_now_recognition));
                            captureButton.setVisibility(View.INVISIBLE);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    shotHandler.post(shotRun = new Runnable(){
                                        @Override
                                        public void run() {
                                            //ContinueShot();
                                        }
                                    });
                                }
                            }).start();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {//結果取得スレッド
                                    getResultHandler.post(getResultRun = new Runnable(){
                                        @Override
                                        public void run() {
                                            getRunnnig = true;
                                            GetResult();
                                        }
                                    });
                                }
                            }).start();

                            saveLog.start();//ログ保存開始

                        }
                    });
        }else if(!checkCameraHardware(this)){
            Log.d("カメラの確認", "カメラの存在確認できません" );
        }

        //開始時(nowLevel=0)で設定画面に遷移
        Intent intent = new Intent(getApplication(), Setting.class);
        intent.putExtra("nowLevel",nowLevel);
        startActivityForResult(intent,1001);
    }


    @Override
    public void onStart(){
        super.onStart();
        Log.v("LifeCycle_MainActivity", "onStart");
        if(ShotFlag) {
            // Create an instance of Camera
            mCamera = getCameraInstance();

            // カメラ映像のプレビュー作成(撮影に必要)
            mPreview = new CameraPreview(this, mCamera);

            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mPreview);

            //progressBar.setVisibility(View.INVISIBLE);
            captureButton.setVisibility(View.VISIBLE);

            ShotFlag = false;
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.d("LifeCycle_MainActivity", "onPause");
        if(ShotFlag) {
            shotHandler.removeCallbacks(shotRun);
            getResultHandler.removeCallbacks(shotRun);
            //saveLogHandler.removeCallbacks(saveLogtRun);
            mPreview.surfaceDestroyed(mPreview.returnHolder());
        }
    }


    protected void onActivityResult( int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(resultCode == RESULT_OK && requestCode == 1001 && intent != null) {
            nowLevel = intent.getIntExtra("nowLevel",0);
        }
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
            e.printStackTrace();
        }

        return c; // returns null if camera is unavailable
    }

    public void ContinueShot(){
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                ByteArrayInputStream imageInput = new ByteArrayInputStream(data);
                theImage = BitmapFactory.decodeStream(imageInput);
                Log.d("撮影1回完了","　");

                uploadTaskSSL = new UploadTaskSSL();
                uploadTaskSSL.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Param(url, theImage));

                //uploadTask = new UploadTask();
                //uploadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Param(url, theImage));

                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(100); // 0.1秒待機
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                });
                mCamera.startPreview();//SurfaceViewの描画更新
                ContinueShot();//次の撮影
            }
        });
    }

    public void GetResult(){
        //サーバの認識結果を確認する処理 GetResultTask
        //認識結果確認用インスタンスの生成
        getResultTaskSSL = new GetResultTaskSSL();
        getResultTaskSSL.setListener(g_createListenerSSL());
        getResultTaskSSL.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Param(url_get, theImage));

        /*
        getResultTask = new GetResultTask();
        getResultTask.setListener(g_createListener());
        getResultTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Param(url_get, theImage));
        */
    }

    //――――――――――――――――――――――――――――――――――――――――――――認識結果確認 HTTPS接続時使用 研究室用――――――――――――――――――――――――――――――――――――――――――――――――――
    private GetResultTaskSSL.Listener g_createListenerSSL() {
        return new GetResultTaskSSL.Listener() {
            @Override
            public void onSuccess(String result){

                if(result.equals("null")){
                    Log.d("SystemCheck", "認識結果が返ってきました" + result);
                }

                if(getRunnnig && !result.equals("null")) {//撮影中は取得
                    Log.d("SystemCheck", "------------認識結果が返ってきました----------------" + result);

                    //医療機器は認識されたが医療行為は特定できず
                    if (return_result.size() == 0 && result.equals("unknown")) {
                        Log.d("認識中", "医療機器は認識されたが医療行為特定には情報不足");
                    }else{//何らかの医療行為が特定された場合
                        if (return_result.get(result) == null) { //初の認識結果なら１を追加
                            return_result.put(result, 1);
                        }else{ //既に追加されてる結果は＋1
                            return_result.put(result, return_result.get(result) + 1);
                        }
                        if(return_result.size() == 0){
                            Log.d("情報提示", "特定された医療行為の情報を提示します(初特定)");
                            now_info = result;
                            setInfo(now_info);
                            displayInfoFlag = true;
                        }else{
                            if(result.equals(now_info)){ //提示中の医療行為が再度認識されている場合
                                Log.d("情報変更無し", "提示中の情報と同じ結果のため変更なしと判断");
                            }else if(result.equals("unknown")) {
                                if(displayInfoFlag) {
                                    Log.d("再認識", "提示している情報の信頼性が低下したため再認識に移行");
                                    iryo_name.setText(getResources().getString(R.string.iryo_now_recognition_anew));
                                    alert_level.setTextColor(getResources().getColor(R.color.hud_white));
                                    alert_level.setText(getResources().getString(R.string.alertLevel_default));
                                    attention_info.setTextColor(getResources().getColor(R.color.hud_white));
                                    attention_info.setText("");
                                    stopInfo();
                                    now_info = null;
                                    displayInfoFlag = false;
                                }
                            }else{ //医療行為を取得した処理(再特定)
                                Log.d("情報提示", "特定された医療行為の情報を提示します");
                                now_info = result;
                                setInfo(now_info);
                                displayInfoFlag = true;
                            }
                        }
                    }

                    Log.d("result", "認識結果数:" + return_result.size());
                    Log.d("return_result", "認識結果蓄積状況:" + return_result);

                }
                GetResult();
            }
        };
    }
    //――――――――――――――――――――――――――――――――――――――――――――認識結果確認 HTTPS接続時使用 研究室用――――――――――――――――――――――――――――――――――――――――――――――――――


    //――――――――――――――――――――――――――――――――――――――――――――認識結果確認 HTTP接続時使用 現地用――――――――――――――――――――――――――――――――――――――――――――――――――
    private GetResultTask.Listener g_createListener() {
        return new GetResultTask.Listener() {
            @Override
            public void onSuccess(String result){
                if(result.equals("null")){
                    Log.d("SystemCheck", "認識結果が返ってきました" + result);
                }

                if(getRunnnig && !result.equals("null")) {//撮影中は取得
                    Log.d("SystemCheck", "------------認識結果が返ってきました----------------" + result);

                    //医療機器は認識されたが医療行為は特定できず
                    if (return_result.size() == 0 && result.equals("unknown")) {
                        Log.d("認識中", "医療機器は認識されたが医療行為特定には情報不足");
                    }else{//何らかの医療行為が特定された場合
                        if (return_result.get(result) == null) { //初の認識結果なら１を追加
                            return_result.put(result, 1);
                        }else{ //既に追加されてる結果は＋1
                            return_result.put(result, return_result.get(result) + 1);
                        }
                        if(return_result.size() == 0){
                            Log.d("情報提示", "特定された医療行為の情報を提示します(初特定)");
                            now_info = result;
                            setInfo(now_info);
                            displayInfoFlag = true;
                        }else{
                            if(result.equals(now_info)){ //提示中の医療行為が再度認識されている場合
                                Log.d("情報変更無し", "提示中の情報と同じ結果のため変更なしと判断");
                            }else if(result.equals("unknown")) {
                                if(displayInfoFlag) {
                                    Log.d("再認識", "提示している情報の信頼性が低下したため再認識に移行");
                                    iryo_name.setText(getResources().getString(R.string.iryo_now_recognition_anew));
                                    alert_level.setTextColor(getResources().getColor(R.color.hud_white));
                                    alert_level.setText(getResources().getString(R.string.alertLevel_default));
                                    attention_info.setTextColor(getResources().getColor(R.color.hud_white));
                                    attention_info.setText("");
                                    stopInfo();
                                    now_info = null;
                                    displayInfoFlag = false;
                                }
                            }else{ //医療行為を取得した処理(再特定)
                                Log.d("情報提示", "特定された医療行為の情報を提示します");
                                now_info = result;
                                setInfo(now_info);
                                displayInfoFlag = true;
                            }
                        }
                    }

                    Log.d("result", "認識結果数:" + return_result.size());
                    Log.d("return_result", "認識結果蓄積状況:" + return_result);

                }
                GetResult();
            }
        };
    }
    //――――――――――――――――――――――――――――――――――――――――――――認識結果確認 HTTP接続時使用 現地用――――――――――――――――――――――――――――――――――――――――――――――――――

    //情報提示プログラム実行用関数
    public void setInfo(String result){

        //骨髄穿刺が結果として返された場合
        if(result.contains("kotuzui")){
            //ProgressBarを不可視
            //progressBar.setVisibility(View.INVISIBLE);
            kotuzui.run(nowLevel,mainHandler);
        }

        //腰椎穿刺が結果として返された場合
        if(result.contains("youtui")){
            //ProgressBarを不可視
            //progressBar.setVisibility(View.INVISIBLE);
            youtui.run(nowLevel,mainHandler);
        }

        //中心静脈カテーテル挿入が結果として返された場合
        if(result.contains("catheter")){
            //ProgressBarを不可視
            //progressBar.setVisibility(View.INVISIBLE);
            catheter.run(nowLevel,mainHandler);
        }

        //血液培養ボトルが結果として返された場合
        if(result.contains("blood")){
            //ProgressBarを不可視
            //progressBar.setVisibility(View.INVISIBLE);
            blood.run(nowLevel,mainHandler);
        }
    }

    //情報提示プログラム停止用関数
    public void stopInfo(){
        if (now_info.equals("kotuzui")) {
            //情報提示用マルチスレッドを中断
            kotuzui.stopThread();
            //骨髄穿刺の注意喚起情報を提示するクラスのインスタンス
            kotuzui = new SetInfo_kotuzui(MainActivity.this);
        } else if (now_info.equals("youtui")) {
            //情報提示用マルチスレッドを中断
            youtui.stopThread();
            //腰椎穿刺の注意喚起情報を提示するクラスのインスタンス
            youtui = new SetInfo_youtui(MainActivity.this);
        } else if (now_info.equals("catheter")) {
            //情報提示用マルチスレッドを中断
            catheter.stopThread();
            //中心静脈カテーテル挿入の注意喚起情報を提示するクラスのインスタンス
            catheter = new SetInfo_catheter(MainActivity.this);
        } else if (now_info.equals("blood")) {
            //情報提示用マルチスレッドを中断
            blood.stopThread();
            //血液培養の注意喚起情報を提示するクラスのインスタンス
            blood = new SetInfo_blood(MainActivity.this);
        }
    }


    @Override
    protected boolean onCreateActionMenu(Menu menu) {
        super.onCreateActionMenu(menu);
        return  true;
    }

    @Override
    protected boolean alwaysShowActionMenu() {
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("LifeCycleCheck", "End Application");
    }
}