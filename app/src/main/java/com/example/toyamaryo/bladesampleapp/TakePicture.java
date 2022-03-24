package com.example.toyamaryo.bladesampleapp;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.Log;

public class TakePicture extends AsyncTask<Integer, Void, Integer> {

    private MainActivity mActivity = new MainActivity();
    private Camera mCamera;
    private Camera.PictureCallback mPicture;
    public int picture_num;

    // コンストラクタ
    public TakePicture(Camera camera, Camera.PictureCallback mPicture) {
        Log.d("TakePictureクラスコンストラクタ","画像撮影用クラス，コンストラクタ生成");
        this.mCamera = camera;
        this.mPicture = mPicture;
    }

    // バックグランドで行う処理
    @Override
    protected Integer doInBackground(Integer... picture_count) {
        Log.d("画像撮影バックグランド処理","画像撮影を開始");
        mCamera.takePicture(null, null, mPicture);

        picture_num = picture_count[0] + 1;
        //Log.d("サーバアップロード後のbitmap2", "bitmap2 : " + bitmap2);
        return picture_count[0] + 1;
    }

    // バックグランド処理が完了し、UIスレッドに反映する
    @Override
    protected void onPostExecute(Integer picture_count) {
        Log.d("撮影完了，MainActivityに移行", "picture_count:" + picture_count);
        mActivity.setPictureCount(picture_count);
    }
}
