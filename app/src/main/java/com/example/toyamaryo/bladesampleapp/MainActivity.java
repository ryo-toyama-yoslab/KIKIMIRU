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
    private ImageView taked_picure;
    private ImageView encoded_bitmap;

    private UploadTask uploadTask;
    private TextView iryo_name;
    private TextView alert_level;
    private TextView attention_info;
    private TextView returnText;
    // wordを入れる
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

    //phpがPOSTで受け取ったwordを入れて作成するHTMLページ(適宜合わせてください)
     String url = "https://grapefruit.sys.wakayama-u.ac.jp/~toyama/sample.php";
     int nowLevel = 0;
     List<String> result_list = new ArrayList();
     Timer timer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iryo_name = findViewById(R.id.iryou_name);
        alert_level = findViewById(R.id.alert_level);
        taked_picure = findViewById(R.id.taked_picture);
        returnText = findViewById(R.id.return_text);
        attention_info = findViewById(R.id.attention_info);

        picture_count = 0;

        soundPlayer = new SoundPlayer(this);


        if(nowLevel == 1){
            attention_info.setTextColor(getResources().getColor(R.color.hud_yellow));
        }else if(nowLevel == 2){
            attention_info = findViewById(R.id.attention_info);
            attention_info.setTextColor(getResources().getColor(R.color.hud_blue));
        }else if(nowLevel == 3){
            attention_info = findViewById(R.id.attention_info);
            attention_info.setTextColor(getResources().getColor(R.color.hud_red));
        }

        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
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

        // 変更テキスト送信ボタンの割り当て
        Button post_btn = findViewById(R.id.post_text);
        post_btn.setOnClickListener(new View.OnClickListener() {
                      @Override
                      public void onClick(View v) {
                          if(url.length() != 0){
                              // ボタンをタップして非同期処理を開始
                              uploadTask = new UploadTask(MainActivity.this);
                              // Listenerを設定
                              uploadTask.setListener(createListener());
                              uploadTask.execute(new Param(url, bitmap2));
                          }
                      }
                  }
        );

        // ボタン割り当て
        Button btn = findViewById(R.id.button);
        btn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        //mCamera.takePicture(null, null, mPicture);
                    }
                }
        );

        // Add a listener to the Capture button
        if(checkCameraHardware(this)==true) {
            Log.d("カメラの確認", "カメラの存在確認できました！" );
            final Button captureButton = findViewById(R.id.button_capture);
            captureButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // get an image from the camera
                            //mCamera.takePicture(null, null, mPicture);
                            take_picture();
                            captureButton.setVisibility(View.INVISIBLE);
                        }
                    }
            );
        }

    }


    protected void onActivityResult( int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(resultCode == RESULT_OK && requestCode == 1001 && intent != null) {
            nowLevel = intent.getIntExtra("nowLevel",0);
            //Log.d("returnLevelは",Integer.toString(nowLevel));
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
            //File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            //if (pictureFile == null){
            //    Log.d(TAG, "Error creating media file, check storage permissions");
            //    return;
            //}

            try {
                //FileOutputStream fos = new FileOutputStream(pictureFile);
                //Log.d("画像データ", data.toString());
                ByteArrayInputStream imageInput = new ByteArrayInputStream(data);

                theImage = BitmapFactory.decodeStream(imageInput);
                bitmap2 = Bitmap.createScaledBitmap(theImage, 416, 416, false);

                //写真確認用
                taked_picure.setImageBitmap(bitmap2);
                returnText.setText("認識を開始する！");

                //写真撮影後，すぐにサーバにアップロード
                uploadTask = new UploadTask(MainActivity.this);
                uploadTask.setListener(createListener());
                uploadTask.execute(new Param(url, bitmap2));
                //encoded_bitmap.setImageBitmap(theImage);

                //fos.write(data);
                //fos.close();

            }catch (Exception e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            }// catch (IOException e) {
            //    Log.d(TAG, "Error accessing file: " + e.getMessage());
            //}
        }
    };

    /*画像をファイルに保存

    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }


    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //Log.d("タイムスタンプ：", timeStamp);
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        }else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        }else{
            return null;
        }

        return mediaFile;
    }

*/

    //認識結果が返ってくる
    private UploadTask.Listener createListener() {
        return new UploadTask.Listener() {
            @Override
            public void onSuccess(String result) {

                if(picture_count < 10){
                    //attention_info.setText(result);
                    Log.d("retake_check", "送信画像が10枚以下のため再撮影:" + String.valueOf(picture_count));
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    Log.d("retake_check", "現在時刻:" + timeStamp);
                    take_picture();
                }else{
                    attention_info.setText(result);
                    Log.d("retake_check", "送信画像が10枚のため再撮影終了" + String.valueOf(picture_count));
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    Log.d("retake_check", "現在時刻:" + timeStamp);
                    picture_count = 0;
                    take_picture();
                }
                //骨髄穿刺針の注意喚起情報表示
                /*if(result.contains("dog")){
                    try{
                        Thread.sleep(2000);
                    }catch(InterruptedException e) {
                    }
                    attention_info.setTextColor(getResources().getColor(R.color.hud_yellow));
                    attention_info.setText(getResources().getString(R.string.mark_level1));
                    if(attention_info.getText().toString().equals(getResources().getString(R.string.mark_level1))){//表示が終わってから5秒待機
                        try{
                            Log.d("今の注意喚起情報は",attention_info.getText().toString());
                            Thread.sleep(5000);

                        }catch(InterruptedException e) {
                        }
                    }
                    attention_info.setTextColor(getResources().getColor(R.color.hud_blue));
                    attention_info.setText(getResources().getString(R.string.mark_level2));
                    if(attention_info.getText().equals(getResources().getString(R.string.mark_level2))){//表示が終わってから5秒待機
                        try{
                            Thread.sleep(5000);

                        }catch(InterruptedException e) {
                        }
                    }
                    attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                    attention_info.setText(getResources().getString(R.string.mark_level3));
                }
                */
                //レベル1に設定した場合
                if(nowLevel == 1){
                    //骨髄穿刺針の注意喚起情報表示
                    if(result.contains("dog")){
                        alert_level.setText(R.string.alertLevel_one);
                        alert_level.setTextColor(getResources().getColor(R.color.hud_yellow));
                        iryo_name.setText(R.string.iryo_name_Kotuzuisennsi);
                        soundPlayer.playLevel1Sound();
                        attention_info.setTextColor(getResources().getColor(R.color.hud_yellow));
                        attention_info.setText(getResources().getString(R.string.mark_level1));
                        /*
                        try{
                            Thread.sleep(5000);
                        }catch(InterruptedException e) {
                        }
                        soundPlayer.playLevel2Sound();
                        attention_info.setTextColor(Color.rgb(255,165,0));//(getResources().getColor(R.color.hud_blue));
                        attention_info.setText(getResources().getString(R.string.mark_level2));
                        try{
                            Thread.sleep(5000);
                        }catch(InterruptedException e) {
                        }
                        soundPlayer.playLevel3Sound();
                        attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                        attention_info.setText(getResources().getString(R.string.mark_level3));
                        */
                    }
                    //腰椎穿刺針の注意喚起情報表示
                    if(result.contains("spinal_needle")){
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
                        soundPlayer.playLevel3Sound();
                        attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                        attention_info.setText(getResources().getString(R.string.blood_cl_bottle_level3));
                    }
                }

                //レベル2に設定した場合
                else if(nowLevel == 2){
                    //骨髄穿刺針の注意喚起情報表示
                    if(result.contains("dog")){
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
                        soundPlayer.playLevel3Sound();
                        attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                        attention_info.setText(getResources().getString(R.string.spinal_level3));
                    }
                    //中心静脈カテーテル挿入の注意喚起情報表示
                    if(result.contains("central_venous_catheter") && result.contains("guide_wire")){
                        soundPlayer.playLevel3Sound();
                        attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                        attention_info.setText(getResources().getString(R.string.central_catheter_in_level3));
                    }
                    //血液培養ボトルの注意喚起情報表示
                    if(result.contains("blood_cl_bottle_orange") && result.contains("blood_cl_bottle_blue")){
                        soundPlayer.playLevel3Sound();
                        attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                        attention_info.setText(getResources().getString(R.string.blood_cl_bottle_level3));
                    }
                }

                //レベル3に設定した場合
                else if(nowLevel == 3){
                    alert_level.setText(R.string.alertLevel_three);
                    alert_level.setTextColor(getResources().getColor(R.color.hud_red));
                    //骨髄穿刺針の注意喚起情報表示
                    if(result.contains("dog")){
                        soundPlayer.playLevel3Sound();
                        iryo_name.setText(R.string.iryo_name_Kotuzuisennsi);
                        attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                        attention_info.setText(getResources().getString(R.string.mark_level3));
                    }
                    //腰椎穿刺針の注意喚起情報表示
                    if(result.contains("spinal_needle")){
                        soundPlayer.playLevel3Sound();
                        attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                        attention_info.setText(getResources().getString(R.string.spinal_level3));
                    }
                    //中心静脈カテーテル挿入の注意喚起情報表示
                    if(result.contains("central_venous_catheter") && result.contains("guide_wire")){
                        soundPlayer.playLevel3Sound();
                        attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                        attention_info.setText(getResources().getString(R.string.central_catheter_in_level3));
                    }
                    //血液培養ボトルの注意喚起情報表示
                    //if(result.contains("blood_cl_bottle_orange") && result.contains("blood_cl_bottle_blue")){
                    if(result.contains("blood_cl_bottle_orange") && result.contains("blood_cl_bottle_blue")){
                        soundPlayer.playLevel3Sound();
                        attention_info.setTextColor(getResources().getColor(R.color.hud_red));
                        attention_info.setText(getResources().getString(R.string.blood_cl_bottle_level3));
                    }
                }else{
                    soundPlayer.playLevel2Sound();
                    soundPlayer.playLevel3Sound();
                    attention_info.setTextColor(getResources().getColor(R.color.hud_white));
                    attention_info.setText("アラートレベルを設定してください");
                }


                //take_picture();
            }
        };
    }

    public int getPictureCount(){
        return picture_count;
    }

    public static void main(String[] args) {
        File file = new File("/内部ストレージ//java/*.txt");

        //deleteメソッドを使用してファイルを削除する
        file.delete();
    }

    @Override
    protected boolean onCreateActionMenu(Menu menu) {
        super.onCreateActionMenu(menu);

        //getMenuInflater().inflate(R.menu.menu, menu);

        //HelloMenuItem = menu.findItem(R.id.item1);
        //VuzixMenuItem = menu.findItem(R.id.item2);
        //BladeMenuItem = menu.findItem(R.id.item3);
        //mainText = findViewById(R.id.mainTextView);

        return true;
    }


    @Override
    protected boolean alwaysShowActionMenu() {
        return false;
    }
    /*
        private void updateMenuItems() {
            if (HelloMenuItem == null) {
                return;
            }

            VuzixMenuItem.setEnabled(false);
            BladeMenuItesetEnabled(false);
        }


    /*
        //Action Menu Click events
        //This events where register via the XML for the menu definitions.
        public void showHello(MenuItem item){

            showToast("Hello World!");
            mainText.setText("Hello World!");
            VuzixMenuItem.setEnabled(false);
            BladeMenuItem.setEnabled(false);
        }

        public void showVuzix(MenuItem item){

        }

        public void showBlade(MenuItem item){
            showToast("Blade");
            mainText.setText("Blade");
        }

        private void showToast(final String text){

            final Activity activity = this;

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    */
    @Override
    protected void onDestroy() {
        uploadTask.setListener(null);
        super.onDestroy();
    }

    /*
    private UploadTask.Listener createListener() {
        return new UploadTask.Listener() {
            @Override
            public void onSuccess(String result) {
                textView.setText(result);
            }
        };
    }
    */
}