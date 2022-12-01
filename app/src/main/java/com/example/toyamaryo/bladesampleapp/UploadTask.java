package com.example.toyamaryo.bladesampleapp;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class UploadTask extends AsyncTask<Param, Void, String> {

    // 非同期処理
    @Override
    protected String doInBackground(Param... params) {
        Param param = params[0];

        // 使用するサーバーのURLに合わせる
        String urlSt = param.uri;
        String img = "img=";

        //HttpsURLConnection httpConn = null;
        HttpURLConnection httpConn = null;
        StringBuilder sb = new StringBuilder();

        try{
            //BitmapをBase64にエンコード
            ByteArrayOutputStream jpg = new ByteArrayOutputStream();
            param.bmp.compress(Bitmap.CompressFormat.JPEG, 100, jpg);
            byte[] b = jpg.toByteArray();
            String imageEncoded = Base64.encodeToString(b,Base64.DEFAULT);
            img = img + imageEncoded.trim();

            // URL設定
            URL url = new URL(urlSt);

            // HttpURLConnection
            //httpConn = (HttpsURLConnection) url.openConnection();
            httpConn = (HttpURLConnection)url.openConnection();

            // request POST
            httpConn.setRequestMethod("POST");

            // request GET
            //httpConn.setRequestMethod("GET");

            // no Redirects
            httpConn.setInstanceFollowRedirects(false);

            // データを書き込む(書き込まない，GETの場合はfalseに)
            httpConn.setDoOutput(true);

            // 時間制限
            httpConn.setReadTimeout(10000000);
            httpConn.setConnectTimeout(200000000);

            httpConn.setRequestProperty("Accept-Language", "jp");

            //レスポンスのボディ受信を許可(許可しないならfalseに)
            httpConn.setDoInput(true);

            // 接続
            httpConn.connect();


            try(// POSTデータ送信処理
                //Stringデータ送信パターン

                OutputStream outStream = httpConn.getOutputStream()){
                outStream.write(img.getBytes(StandardCharsets.ISO_8859_1));
                outStream.flush();

                InputStream is = httpConn.getInputStream();
                is.close();
                //Log.d(TAG, "ReturnFromServer " + sb.toString());

            } catch (IOException e) {
                // POST送信エラー
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }

        return sb.toString();
    }

    // 非同期処理が終了後、結果をメインスレッドに返す
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
    }

    interface Listener {
        void onSuccess(String result);
    }
}