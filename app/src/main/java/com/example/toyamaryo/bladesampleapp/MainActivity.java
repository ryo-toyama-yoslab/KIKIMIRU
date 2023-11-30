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

    private Handler mainHandler;
    private Handler shotHandler;
    private Runnable shotRun;
    private Handler shotDelayHandler;
    private Runnable shotDelayRun;
    private Handler getResultHandler;
    private Runnable getResultRun;
    private Handler getResultDelayHandler;
    private Runnable getResultDelayRun;
    private Handler saveLogHandler;
    private Runnable saveLogRun;

    private boolean getRunnig = false;//認識結果取得実行用フラグ
    private boolean displayInfoFlag = false; // 情報が表示されているかのフラグ

    // 実行環境の切り替え用 debug : 研究室, prod : 本番利用サーバ
    public String run_env = "debug";

    //仲介用phpのアドレス(grapefruitサーバ用，SSL)
    private String url = "";
    private String url_get = "";

    // 設定されている状態
    private int experimentMode; // 実験手法切り替え　1 : 通知音無し, 2 : 機械音通知, 3 : 音声通知
    private int nowLevel; // 手技熟練度の設定

    // Sound設定
    public SoundPlayer soundPlayer;

    //現在提示している情報
    public String now_info = "";

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

    Intent setting_intent;
    Intent mode_intent;

    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // UIオブジェクトを取得
        iryo_name = findViewById(R.id.iryou_name);
        alert_level = findViewById(R.id.alert_level);
        attention_info = findViewById(R.id.attention_info);
        captureButton = findViewById(R.id.button_capture);
        Button setting_btn = findViewById(R.id.setting_button);
        Button end_btn = findViewById(R.id.end_button);

        iryo_name.setVisibility(View.INVISIBLE); // 医療行為名を非表示
        alert_level.setVisibility(View.INVISIBLE); // アラートレベルを表示
        attention_info.setVisibility(View.INVISIBLE); // 注意喚起情報を表示


        experimentMode = 0; // ユーザの選択した経験量レベル
        nowLevel = 0; // ユーザの選択した経験量レベル

        // 情報表示オブジェクトの設定変更スレッド
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 手技経験量別の設定変更
                if(nowLevel == 1){
                    attention_info.setTextColor(getResources().getColor(R.color.hud_yellow));
                }else if(nowLevel == 2){
                    attention_info.setTextColor(Color.argb(20,255,69,0));
                }else if(nowLevel == 3){
                    attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                }
            }
        }).start();

        // Handler生成(handlerはThread内で生成できない)
        mainHandler = new Handler(); // 情報提示中の中断通知用Handlerを生成
        shotHandler = new Handler(); // 撮影スレッドの停止用Handlerを生成
        shotDelayHandler = new Handler(); // 撮影スレッドを定期実行用
        getResultHandler = new Handler(); // 結果取得用スレッドの停止用Handlerを生成
        getResultDelayHandler = new Handler(); // 結果取得を定期実行するための
        saveLogHandler = new Handler(); // ログ保存スレッドの停止用Handlerを生成

        // ログ初期化スレッド
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    Runtime.getRuntime().exec(new String[] { "logcat", "-c"}); //以前のログをクリア
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

        // 実行環境のモード切替
        switch(run_env){
            case "debug":
                Log.d("SSL用URIを設定","debug mode ,so insert grapefruit url_get");
                url = "https://grapefruit.sys.wakayama-u.ac.jp/~toyama/kikimiru_server/getImage.php";
                url_get = "https://grapefruit.sys.wakayama-u.ac.jp/~toyama/kikimiru_server/returnRecognitionResult.php";
                break;
            case "prod":
                Log.d("医大用URIを設定","prod mode ,so insert idai url_get");
                url = "http://172.30.184.57/~toyama/kikimiru_server/getImage.php";
                url_get = "http://172.30.184.57/~toyama/kikimiru_server/returnRecognitionResult.php";
                break;
        }

        // カメラ初期化
        mCamera = getCameraInstance(); // カメラインスタンス生成
        mPreview = new CameraPreview(MainActivity.this, mCamera); // カメラ映像のプレビュー作成(撮影に必要)
        FrameLayout preview = (FrameLayout)findViewById(R.id.camera_preview); // プレビューオブジェクト取得
        preview.addView(mPreview); // プレビュー追加

        // インスタンス生成スレッド
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 注意喚起情報変更クラスのインスタンス生成(骨髄穿刺)
                kotuzui = new SetInfo_kotuzui(MainActivity.this);
                // 注意喚起情報変更クラスのインスタンス生成(腰椎穿刺)
                youtui = new SetInfo_youtui(MainActivity.this);
                // 注意喚起情報変更クラスのインスタンス生成(中心静脈カテーテル挿入)
                catheter = new SetInfo_catheter(MainActivity.this);
                // 注意喚起情報変更クラスのインスタンス生成(血液培養)
                blood = new SetInfo_blood(MainActivity.this);
                // ログ保存クラスのインスタンス生成
                saveLog = new SaveLog();
                // 通知音再生クラスのインスタンス生成
                soundPlayer = new SoundPlayer(MainActivity.this);
            }
        }).start();

        // 設定画面に遷移
        setting_btn.setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
               if(stopInfo()) {
                   Log.i("情報提示中断", "設定画面に遷移します");
                   Log.d("postLevel", Integer.toString(nowLevel));
               }
               now_info = "setting"; // 設定画面遷移
               Intent setting_intent = new Intent(getApplication(), Setting.class);
               setting_intent.putExtra("nowLevel",nowLevel);
               Log.d("SystemCheck","startActivityを行う前です");
               startActivityForResult(setting_intent,1002);
            }
        });

        // 終了時処理
        end_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("EndButton", "終了ボタンが押されました");
                try {
                    if(stopInfo()) {
                        Log.d("情報提示スレッド停止", "成功");
                    }
                } catch (Exception e) {
                    Log.e("情報提示スレッド停止エラー", e.toString());
                    e.printStackTrace();
                }

                getRunnig = false; // 情報変更処理を実行されなくする
                moveTaskToBack(true); // アプリケーション全体を中断 onDestroy()が呼ばれなければonRestart()で再開

                // 非同期処理リスナー解放
                try{
                    switch (run_env) {
                        case "debug":
                            getResultTaskSSL.removeListener(g_createListenerSSL());
                            break;
                        case "prod":
                            getResultTask.removeListener(g_createListener());
                            break;
                    }
                    Log.d("認識結果スレッド停止", "成功");
                }catch(Exception e){
                    Log.e("認識結果スレッド停止", "失敗 " + e.toString());
                }


                try{
                    Log.d("アプリが終了するまで5秒 : ", "待機開始");
                    Thread.sleep(5000); // 5秒待機
                } catch (Exception e) {
                    Log.e("アプリ終了待機中エラー", e.toString());
                }
                Log.d("アプリケーション KIKIMIRU: ", "終了します");
                finish(); //アプリケーション終了 再開時はonCreate()から
            }
        });

        // 撮影ボタン押下時処理
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

        // 設定画面を表示
        setting_intent = new Intent(getApplication(), Setting.class);

        // 実行モード画面を表示(設定画面の上)
        mode_intent = new Intent(getApplication(), CheckoutRunMode.class);
        mode_intent.putExtra("experimentMode",experimentMode);
        startActivityForResult(mode_intent,1001);
    }

    @Override
    public void onStart(){ // リソースの確保・UIの更新
        super.onStart();
        Log.v("LifeCycle_MainActivity", "onStart");
        getRunnig = true; // MainUI表示状態に設定
        alert_level.setText("");
        attention_info.setText("");
        iryo_name.setText(getResources().getString(R.string.iryo_name_default));
        captureButton.setFocusableInTouchMode(true);

        // 撮影ボタンにフォーカスしておく
        captureButton.requestFocus();

        if((experimentMode != 0) && (nowLevel == 0)){
            setting_intent.putExtra("nowLevel",nowLevel);
            startActivityForResult(setting_intent,1002);
        }

    }

    @Override
    public void onPause(){ // リソースの解放・非同期タスク停止
        super.onPause();
        Log.d("LifeCycle_MainActivity", "onPose");
        displayInfoFlag = false; // 情報非表示状態に設定
        getRunnig = false; // MainUI非表示状態に設定
    }

    @Override
    public void onStop(){
        super.onStop();
        Log.d("LifeCycle_MainActivity", "onStop");
    }

    protected void onActivityResult( int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(resultCode == RESULT_OK && requestCode == 1001 && intent != null) {
            experimentMode = intent.getIntExtra("experimentMode",0);
        }else if(resultCode == RESULT_OK && requestCode == 1002 && intent != null) {
            nowLevel = intent.getIntExtra("nowLevel",0);
        }
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
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

    private void ContinueShot(){
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                ByteArrayInputStream imageInput = new ByteArrayInputStream(data);
                theImage = BitmapFactory.decodeStream(imageInput);

                Log.d("撮影1回完了", "撮影しました");

                // 実行環境別でクラス変更
                switch (run_env) {
                    case "debug":
                        uploadTaskSSL = new UploadTaskSSL();
                        uploadTaskSSL.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Param(url, theImage));
                        break;
                    case "prod":
                        uploadTask = new UploadTask();
                        uploadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Param(url, theImage));
                        break;
                }

                shotDelayHandler.postDelayed(shotDelayRun = new Runnable() { // 負荷軽減のため0.1秒待機
                    @Override
                    public void run() {
                        try {
                            mCamera.startPreview();//SurfaceViewの描画更新
                            ContinueShot();//次の撮影
                        }catch(Exception e){
                            Log.e("撮影スレッドエラー : ", e.toString());
                            shotDelayHandler.removeCallbacks(shotDelayRun);
                        }
                    }
                }, 100);

            }
        });
    }

    public void GetResult(){
        //サーバの認識結果を確認する処理 GetResultTask
        //認識結果確認用インスタンスの生成

        // 実行環境別でクラス変更
        switch(run_env){
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
                controlInfo(result);
            }
        };
    }
    //――――――――――――――――――――――――――――――――――――――――――――認識結果確認 HTTPS接続時使用 研究室用――――――――――――――――――――――――――――――――――――――――――――――――――

    //――――――――――――――――――――――――――――――――――――――――――――認識結果確認 HTTP接続時使用 現地用――――――――――――――――――――――――――――――――――――――――――――――――――
    private GetResultTask.Listener g_createListener() {
        return new GetResultTask.Listener() {
            @Override
            public void onSuccess(String result){
                controlInfo(result);
            }
        };
    }
    //――――――――――――――――――――――――――――――――――――――――――――認識結果確認 HTTP接続時使用 現地用――――――――――――――――――――――――――――――――――――――――――――――――――

    public void controlInfo(String result){

        Log.d("SystemCheck", "------------認識結果が返ってきました------------ " + result);

        if(getRunnig) { // 撮影中は取得

            if (now_info.isEmpty() && result.equals("unknown")) { // 医療行為は未特定
                Log.d("認識中", "医療行為未特定");
            }else if(!(result.equals("kotuzui") || result.equals("youtui") || result.equals("catheter") || result.equals("blood") || result.equals("unknown"))){
                Log.d("SystemCheck", "想定外の結果が返されました．検出処理に問題があるかもしれません　" + result);
            }else{ // 何らかの医療行為が特定された場合
                if(result.equals(now_info)){ // 提示中の医療行為が再度認識されている場合
                    Log.d("情報変更無し", "提示中の情報と同じ結果のため変更なしと判断");
                }else if(now_info.isEmpty() || now_info.equals("setting")){ // 初回特定 or 手技熟練度設定変更後
                    // 情報通知音
                    if(experimentMode == 2){
                        soundPlayer.playMechanicalSound();
                    }else if(experimentMode == 3){
                        if(result.equals("kotuzui")){
                            soundPlayer.playReadingKotuzuiInfoSound(1);
                        }else {
                            soundPlayer.playDisplayVoiceSound();
                        }
                    }
                    now_info = result;
                    setInfo(now_info);
                    displayInfoFlag = true;
                }else{
                    if(result.equals("unknown")) { // 特定後に特定結果が変更される場合
                        if(displayInfoFlag){
                            Log.d("再認識", "提示している情報の信頼性が低下したため再認識に移行");
                            if(stopInfo()) {
                                iryo_name.setText(R.string.iryo_now_recognition_anew);
                                alert_level.setVisibility(View.INVISIBLE); // アラートレベルを表示
                                attention_info.setVisibility(View.INVISIBLE); // 注意喚起情報を表示
                                now_info = "";
                                displayInfoFlag = false;
                            }
                            // 音声通知の場合 情報変更通知音
                            if (experimentMode == 2) {
                                soundPlayer.playMechanicalSound();
                            }else if(experimentMode == 3){
                                soundPlayer.playCheckingVoiceSound();
                            }
                        }
                    }else{ // 医療行為を取得した処理(再特定)
                        Log.d("情報提示", "特定された医療行為の情報を提示します");
                        if(displayInfoFlag){
                            Log.d("認識結果変更 提示中断", "情報提示中に異なる特定結果を取得 情報提示を中断"); // unknownを取得する前に別の医療行為が特定された場合
                            if(stopInfo()){
                                iryo_name.setVisibility(View.INVISIBLE); // 医療行為名を非表示
                                alert_level.setVisibility(View.INVISIBLE); // アラートレベルを表示
                                attention_info.setVisibility(View.INVISIBLE); // 注意喚起情報を表示
                            }
                        }

                        Log.d("情報変更通知", "特定結果が変更されたの通知"); // unknownを取得する前に別の医療行為が特定された場合
                        // 音声通知の場合 情報変更通知音
                        if (experimentMode == 2) {
                            soundPlayer.playMechanicalSound();
                        } else if (experimentMode == 3) { // 音声通知
                            soundPlayer.playChangeVoiceSound();
                        }

                        now_info = result;
                        setInfo(now_info);
                    }
                }
            }

            Log.d("latest_result", "最新特定結果 : " + now_info);


            getResultDelayHandler.postDelayed(getResultDelayRun = new Runnable() { // 負荷軽減のため0.1秒待機
                @Override
                public void run() {
                    Log.d("debug","認識結果をGETします");
                    GetResult();
                }
            }, 100);
        }
    }

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
    public boolean stopInfo(){
        if(now_info.equals("kotuzui")){
            if(kotuzui.stopThread()){ //情報提示用マルチスレッドを中断
                kotuzui = new SetInfo_kotuzui(MainActivity.this); //注意喚起情報を提示するクラスのインスタンスを再生成
                return true;
            }
        }else if(now_info.equals("youtui")){
            if(youtui.stopThread()) {
                youtui = new SetInfo_youtui(MainActivity.this);
                return true;
            }
        }else if(now_info.equals("catheter")){
            if(catheter.stopThread()) {
                catheter = new SetInfo_catheter(MainActivity.this);
                return true;
            }
        }else if(now_info.equals("blood")){
            if(blood.stopThread()) {
                blood = new SetInfo_blood(MainActivity.this);
                return true;
            }
        }
        return false;
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
        shotHandler.removeCallbacks(shotRun);
        shotHandler = null;
        shotDelayHandler.removeCallbacks(shotDelayRun);
        shotDelayHandler = null;
        getResultHandler.removeCallbacks(getResultRun);
        getResultHandler = null;
        getResultDelayHandler.removeCallbacks(getResultDelayRun);
        getResultDelayHandler = null;
        mPreview.surfaceDestroyed(mPreview.returnHolder());
        saveLogHandler.removeCallbacks(saveLogRun);
        saveLog.stopSaveLog();
    }
}