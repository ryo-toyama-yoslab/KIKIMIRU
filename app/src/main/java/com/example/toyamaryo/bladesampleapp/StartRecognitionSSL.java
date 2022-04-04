package com.example.toyamaryo.bladesampleapp;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static android.content.ContentValues.TAG;

public class StartRecognitionSSL extends AsyncTask<Param, Void, String> {

    private StartRecognitionSSL.Listener listener;
    private MainActivity mainActivity = new MainActivity();


    public StartRecognitionSSL(){
    }

    // 非同期処理
    @Override
    protected String doInBackground(Param... params) {
        Param param = params[0];

        // 使用するサーバーのURLに合わせる
        String urlSt = param.uri;

        //HttpsURLConnection httpConn = null;
        HttpURLConnection httpConn;
        StringBuilder sb = new StringBuilder();

        try{
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

        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("SystemCheck", "サーバからの返ってきた認識結果 : " + sb.toString());
        return sb.toString();
    }

    // 非同期処理が終了後、結果をメインスレッドに返す
    @Override
    protected void onPostExecute(String result) {
        //認識結果のラベル名をテキスト表示
        //super.onPostExecute(result);
    }

    void setListener(StartRecognitionSSL.Listener listener) {
        this.listener = listener;
    }

    interface Listener {
        void onSuccess(String result);
    }
}