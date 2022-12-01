package com.example.toyamaryo.bladesampleapp;

import android.os.AsyncTask;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GetResultTask extends AsyncTask<Param, Void, String>  {


    private GetResultTask.Listener listener;

    // 非同期処理
    @Override
    protected String doInBackground(Param... params) {
        Param param = params[0];

        // 使用するサーバーのURLに合わせる
        String urlSt = param.uri;

        HttpURLConnection httpConn = null;
        StringBuilder sb = new StringBuilder();

        try {
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


            // レスポンスコードの確認します。
            /*
            int responseCode = httpConn.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP responseCode: " + responseCode);
            }*/

            try {
                InputStream is = httpConn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"));
                String line = "";
                while ((line = reader.readLine()) != null)
                    sb.append(line);
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
        if (listener != null) {
            listener.onSuccess(result);
        }
    }

    void setListener(GetResultTask.Listener listener) {
        this.listener = listener;
    }

    void removeListener(GetResultTask.Listener listener){
        this.listener = null;
    }

    interface Listener {
        void onSuccess(String result);
    }
}

