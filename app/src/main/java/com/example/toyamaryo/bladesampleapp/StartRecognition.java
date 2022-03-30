package com.example.toyamaryo.bladesampleapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static android.content.ContentValues.TAG;

public class StartRecognition extends AsyncTask<Param, Void, String> {

    private StartRecognition.Listener listener;
    private MainActivity mainActivity = new MainActivity();
    private int picture_count;


    public StartRecognition(){
    }

    // 非同期処理
    @Override
    protected String doInBackground(Param... params) {
        Param param = params[0];

        // 使用するサーバーのURLに合わせる
        String urlSt = param.uri;
        String path = "path=" + param.str;

        Log.d("SystemCheck", "-----------------------10枚の画像で認識を開始します-------------------------------" + path);

        //HttpsURLConnection httpConn = null;
        HttpURLConnection httpConn;
        StringBuilder sb = new StringBuilder();

        picture_count = mainActivity.getPictureCount();

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


            try(// POSTデータ送信処理
                //Stringデータ送信パターン

                OutputStream outStream = httpConn.getOutputStream()){
                outStream.write(path.getBytes(StandardCharsets.ISO_8859_1));
                outStream.flush();
                //Log.d("debug","flush");
                InputStream is = httpConn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"));
                String line = "";
                while ((line = reader.readLine()) != null)
                    sb.append(line);
                is.close();
                Log.d(TAG, "ReturnFromServer " + sb.toString());

            } catch (IOException e) {
                // POST送信エラー
                e.printStackTrace();
            }

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

        if (listener != null) {
            Log.d("SystemCheck", "サーバからの返ってきた認識結果をMainに返しますーーーーーーーーーーーーーーーーーーーーーーーーーーーーー");
            listener.onSuccess(result);
        }
    }

    void setListener(StartRecognition.Listener listener) {
        this.listener = listener;
    }

    interface Listener {
        void onSuccess(String result);
    }
}

