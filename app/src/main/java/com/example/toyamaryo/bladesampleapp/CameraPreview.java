package com.example.toyamaryo.bladesampleapp;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

import static android.content.ContentValues.TAG;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback{
    private SurfaceHolder mHolder;
    public Camera mCamera;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mHolder.setKeepScreenOn(true);//SurfaceViewが表示されている間は画面を常にオン
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        if(mCamera != null){
            // パラメータ取得
            Camera.Parameters params = mCamera.getParameters();
            // サイズ設定
            params.setPictureSize(480, 480);
            // パラメータ設定
            mCamera.setParameters(params);
            try {
                mCamera.setPreviewDisplay(holder);
                //mCamera.startPreview();
            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }
    }


    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        if(mCamera != null) {
            try {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
                mHolder = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        //プレビュースタート（Changedは最初にも1度は呼ばれる）
        if(mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            params.setPictureFormat(PixelFormat.JPEG);
            List list = params.getSupportedPreviewSizes();

            Log.d("CameraParam",list.toString());

            if (mHolder.getSurface() == null) {
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                Log.d(TAG, "Error stopping camera preview: " + e.getMessage());
            }

            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (Exception e) {
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }


    public SurfaceHolder returnHolder(){
        return mHolder;
    }


}