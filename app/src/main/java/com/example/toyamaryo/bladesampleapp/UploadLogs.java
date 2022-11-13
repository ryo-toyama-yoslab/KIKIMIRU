package com.example.toyamaryo.bladesampleapp;

import android.os.AsyncTask;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;

public class UploadLogs extends AsyncTask<Param, Void, String> {

    // 非同期処理
    @Override
    protected String doInBackground(Param... params) {
        Param param = params[0];

        // 使用するサーバーのURLに合わせる
        String urlSt = param.uri;
        String log = "log=";

        HttpsURLConnection httpConn = null;
        StringBuilder sb = new StringBuilder();

        try{

            //サーバに送信するログ
            String log_str = param.str;
            log += log_str;

            // URL設定
            URL url = new URL(urlSt);

            // HttpURLConnection
            httpConn = (HttpsURLConnection) url.openConnection();

            // request POST
            httpConn.setRequestMethod("POST");

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
                //送信データ確認用
                outStream.write(log.getBytes(StandardCharsets.UTF_8));
                outStream.flush();

                InputStream is = httpConn.getInputStream();
                is.close();

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
        //認識結果のラベル名をテキスト表示
        super.onPostExecute(result);
    }

    interface Listener {
        void onSuccess(String result);
    }
}