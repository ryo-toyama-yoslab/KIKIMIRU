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

import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

public class MainActivity extends ActionMenuActivity{

    //SSL認証サーバとの接続用
    private UploadTaskSSL uploadTaskSSL;

    //非SSL認証サーバとの接続用
    private UploadTask uploadTask;

    //認識結果確認用非同期処理クラスのインスタンス
    private GetResultTaskSSL getResultTaskSSL;
    private GetResultTask getResultTask;

    public TextView iryo_name;
    public TextView alert_level;
    public TextView attention_info;

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
    public Runnable saveLogRun;

    private boolean ShotFlag = false;//撮影を開始しているか
    private boolean getRunnig = false;//認識結果取得実行用フラグ
    private boolean displayInfoFlag = false;

    // 実行環境の切り替え用 debug : 研究室, prod : 本番利用サーバ
    public String run_mode = "debug";

    //仲介用phpのアドレス(grapefruitサーバ用，SSL)
    private String url = "";
    private String url_get = "";

    // 設定されている状態
    private int experimentMode; // 実験手法切り替え　1 : 機械音通知, 2 : 音声通知
    private int nowLevel; // 手技熟練度の設定

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
        Log.d("StartSystem_Log" ,"ReadLog");

        try{
            Runtime.getRuntime().exec(new String[] { "logcat", "-c"}); //以前のログをクリア
        }catch(Exception e){
            e.printStackTrace();
        }

        // 実行環境のモード切替
        switch(run_mode){
            case "debug":
                Log.d("SSL用URIを設定","debug mode ,so insert grapefruit url_get");
                url = "https://grapefruit.sys.wakayama-u.ac.jp/~toyama/kikimiru_server/getImage.php";
                url_get = "https://grapefruit.sys.wakayama-u.ac.jp/~toyama/kikimiru_server/returnRecognitionResult.php";
                break;
            case "prod":
                url = "http://172.30.184.57/~toyama/getImage.php";
                url_get = "http://172.30.184.57/~toyama/returnRecognitionResult.php";
                break;
        }


        setContentView(R.layout.activity_main);

        iryo_name = findViewById(R.id.iryou_name);
        alert_level = findViewById(R.id.alert_level);
        attention_info = findViewById(R.id.attention_info);

        // 情報提示中の中断通知用Handlerを生成
        mainHandler = new Handler();
        // 撮影スレッドの停止用Handlerを生成
        shotHandler = new Handler();
        // 結果取得用スレッドの停止用フラグを生成
        getResultHandler = new Handler();
        // ログ保存スレッドの停止用Handlerを生成
        saveLogHandler = new Handler();

        // ユーザの選択した経験量レベル
        experimentMode = 0;

        // ユーザの選択した経験量レベル
        nowLevel = 0;

        soundPlayer = new SoundPlayer(this);

        // Create an instance of Camera
        mCamera = getCameraInstance();

        // カメラ映像のプレビュー作成(撮影に必要)
        mPreview = new CameraPreview(this, mCamera);

        FrameLayout preview = (FrameLayout)findViewById(R.id.camera_preview);
        preview.addView(mPreview);


        // mPreviewのSurfaceHolder取得用
        //mHolder = mPreview.returnHolder();

        // 手技経験量別の設定変更
        if(nowLevel == 1){
            attention_info.setTextColor(getResources().getColor(R.color.hud_yellow));
        }else if(nowLevel == 2){
            attention_info.setTextColor(Color.argb(20,255,69,0));
        }else if(nowLevel == 3){
            attention_info.setTextColor(getResources().getColor(R.color.hud_red));
        }


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
                   Intent setting_intent = new Intent(getApplication(), Setting.class);
                   setting_intent.putExtra("nowLevel",nowLevel);
                   Log.d("SystemCheck","startActivityを行う前です");
                   startActivityForResult(setting_intent,1002);
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
                       e.printStackTrace();
                   }
                   moveTaskToBack(true); //アプリケーション全体を中断 onDestroy()が呼ばれなければonRestart()で再開
                   getRunnig = false;
                   switch (run_mode) {
                       case "debug":
                           getResultTaskSSL.removeListener(g_createListenerSSL());
                           break;
                       case "prod":
                           getResultTask.removeListener(g_createListener());
                           break;
                   }
                   try {
                       Log.d("アプリが終了するまで5秒 : ","待機開始");
                       Thread.sleep(5000); // 5秒待機
                   }catch (Exception e){
                       Log.d("アプリ終了待機中エラー",e.toString());
                   }
                   Log.d("アプリケーション KIKIMIRU: ","終了します");
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
                            iryo_name.setText(getResources().getString(R.string.iryo_now_recognition));
                            captureButton.setVisibility(View.INVISIBLE);

                            new Thread(new Runnable() {
                                @Override
                                public void run() { // 画像撮影スレッド
                                    shotHandler.post(shotRun = new Runnable(){
                                        @Override
                                        public void run() {
                                            ShotFlag = true;
                                            ContinueShot();
                                        }
                                    });
                                }
                            }).start();

                            new Thread(new Runnable() {
                                @Override
                                public void run() { // 結果取得スレッド
                                    getResultHandler.post(getResultRun = new Runnable(){
                                        @Override
                                        public void run() {
                                            getRunnig = true;
                                            GetResult();
                                        }
                                    });
                                }
                            }).start();

                            new Thread(new Runnable() {
                                @Override
                                public void run() { // ログ保存スレッド
                                    saveLogHandler.post(saveLogRun = new Runnable(){
                                        @Override
                                        public void run() {
                                            saveLog.start(); // ログ保存開始
                                        }
                                    });
                                }
                            }).start();

                        }
                    });
        }else if(!checkCameraHardware(this)){
            Log.d("カメラの確認", "カメラの存在確認できません" );
        }

        //開始時(experimentMode=0) 実行モード画面に遷移
        Intent mode_intent = new Intent(getApplication(), CheckoutRunMode.class);
        mode_intent.putExtra("experimentMode",experimentMode);
        startActivityForResult(mode_intent,1001);
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
            getResultHandler.removeCallbacks(getResultRun);
            mPreview.surfaceDestroyed(mPreview.returnHolder());
            saveLogHandler.removeCallbacks(saveLogRun);
            saveLog.stopSaveLog();
        }
    }


    protected void onActivityResult( int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(resultCode == RESULT_OK && requestCode == 1001 && intent != null) {
            experimentMode = intent.getIntExtra("experimentMode",0);

            //設定画面に遷移
            Intent setting_intent = new Intent(getApplication(), Setting.class);
            setting_intent.putExtra("nowLevel",nowLevel);
            startActivityForResult(setting_intent,1002);
        }
        else if(resultCode == RESULT_OK && requestCode == 1002 && intent != null) {
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

                Log.d("撮影1回完了", "");

                // 実行環境別でクラス変更
                switch (run_mode) {
                    case "debug":
                        uploadTaskSSL = new UploadTaskSSL();
                        uploadTaskSSL.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Param(url, theImage));
                        break;
                    case "prod":
                        uploadTask = new UploadTask();
                        uploadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Param(url, theImage));
                        break;
                }

                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(100); // 0.1秒待機　早すぎて撮影画像がブレる対策
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

        // 実行環境別でクラス変更
        switch(run_mode){
            case "debug":
                getResultTaskSSL = new GetResultTaskSSL();
                getResultTaskSSL.setListener(g_createListenerSSL());
                getResultTaskSSL.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Param(url_get));
                break;
            case "prod":
                getResultTask = new GetResultTask();
                getResultTask.setListener(g_createListener());
                getResultTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Param(url_get));
                break;
        }

    }

    //――――――――――――――――――――――――――――――――――――――――――――認識結果確認 HTTPS接続時使用 研究室用――――――――――――――――――――――――――――――――――――――――――――――――――
    private GetResultTaskSSL.Listener g_createListenerSSL() {
        return new GetResultTaskSSL.Listener() {
            @Override
            public void onSuccess(String result){

                if(result.equals("null")){
                    Log.d("SystemCheck", "認識結果が返ってきました" + result);
                }

                if(getRunnig && !result.equals("null")) {//撮影中は取得
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

                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(1000); // 1秒待機
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                });

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

                if(getRunnig && !result.equals("null")) {//撮影中は取得
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
            kotuzui.run(nowLevel, experimentMode, mainHandler);
        }

        //腰椎穿刺が結果として返された場合
        if(result.contains("youtui")){
            youtui.run(nowLevel, experimentMode, mainHandler);
        }

        //中心静脈カテーテル挿入が結果として返された場合
        if(result.contains("catheter")){
            catheter.run(nowLevel, experimentMode, mainHandler);
        }

        //血液培養ボトルが結果として返された場合
        if(result.contains("blood")){
            blood.run(nowLevel, experimentMode, mainHandler);
        }
    }

    //情報提示プログラム停止用関数
    public void stopInfo(){

        if(now_info == null){
            Log.d("情報提示状態","情報は提示されていません");
            return;
        }

        switch (now_info) {
            case "kotuzui":
                kotuzui.stopThread(); //情報提示用マルチスレッドを中断
                kotuzui = new SetInfo_kotuzui(MainActivity.this); //注意喚起情報を提示するクラスのインスタンスを再生成
            case "youtui":
                youtui.stopThread();
                youtui = new SetInfo_youtui(MainActivity.this);
            case "catheter":
                catheter.stopThread();
                catheter = new SetInfo_catheter(MainActivity.this);
            case "blood":
                blood.stopThread();
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