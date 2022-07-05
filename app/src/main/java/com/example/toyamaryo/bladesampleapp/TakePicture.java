package com.example.toyamaryo.bladesampleapp;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TakePicture extends AsyncTask<Integer, Void, Integer> {

    private MainActivity mActivity = new MainActivity();
    private Camera mCamera;
    private Camera.PictureCallback mPicture;
    public int picture_num;
    //取得する日時のフォーマットを指定
    final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    // コンストラクタ
    public TakePicture(Camera camera, Camera.PictureCallback mPicture) {
        Log.d("TakePictureクラスコンストラクタ","画像撮影用クラス，コンストラクタ生成");
        this.mCamera = camera;
        this.mPicture = mPicture;
    }

    // バックグランドで行う処理
    @Override
    protected Integer doInBackground(Integer... picture_count) {
        mCamera.takePicture(null, null, mPicture);
        picture_num = picture_count[0] + 1;
        Log.d("画像撮影バックグランド処理", "画像撮影を実行");

        return picture_num;
    }

    // バックグランド処理が完了し、UIスレッドに反映する
    @Override
    protected void onPostExecute(Integer picture_count) {
        Log.d("撮影完了，MainActivityに移行", "picture_count : " + picture_count);
        /*
        final Date date = new Date(System.currentTimeMillis());
        //日時を指定したフォーマットで取得
        Log.d("現在時刻", "CurrentTime : " + df.format(date));
        */
        mActivity.setPictureCount(picture_count);
    }

    /** cancel() がコールされると呼び出される。 */
    @Override
    protected void onCancelled() {
        // 結果を表示 "タスク名 - cancel() が呼ばれました。"
        Log.d("onCancelled()","TakePictureのバックグランド処理を終了");
    }

}
