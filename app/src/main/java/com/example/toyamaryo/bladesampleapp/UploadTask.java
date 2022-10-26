package com.example.toyamaryo.bladesampleapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HttpsURLConnection;

import static android.content.ContentValues.TAG;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;
import static org.apache.http.conn.ssl.SSLSocketFactory.SSL;


public class UploadTask extends AsyncTask<Param, Void, String> {

    private UploadTask.Listener listener;
    private MainActivity mainActivity = new MainActivity();
    private Bitmap decodedByte;
    private int picture_count;



    public UploadTask(){
    }


    // 非同期処理
    @Override
    protected String doInBackground(Param... params) {
        Log.d("SystemCheck", "サーバへのアップロード中です");
        Param param = params[0];

        // 使用するサーバーのURLに合わせる
        String urlSt = param.uri;
        String img = "img=";

        //HttpsURLConnection httpConn = null;
        HttpURLConnection httpConn;
        StringBuilder sb = new StringBuilder();

        picture_count = mainActivity.getPictureCount();

        try{
            //BitmapをBase64にエンコード
            ByteArrayOutputStream jpg = new ByteArrayOutputStream();
            param.bmp.compress(Bitmap.CompressFormat.JPEG, 100, jpg);
            byte[] b = jpg.toByteArray();
            String imageEncoded = Base64.encodeToString(b,Base64.DEFAULT);
            img = img + imageEncoded.trim();

            //確認用：Base64をBitmapにデコード
            byte[] decodedString = Base64.decode(imageEncoded.trim(), Base64.DEFAULT);
            decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);


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
                //送信データ確認用
                //Log.d("画像Byteデータ",img);
                outStream.write(img.getBytes(StandardCharsets.ISO_8859_1));
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
            int status = httpConn.getResponseCode();
            Log.d(TAG, "httpConnectStatus " + status);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    // 非同期処理が終了後、結果をメインスレッドに返す
    @Override
    protected void onPostExecute(String result) {
        //認識結果のラベル名をテキスト表示
        //super.onPostExecute(result);
        Log.d("SystemCheck", "サーバへの画像送信状況 : " + result);

        if (listener != null) {
            Log.d("SystemCheck", "サーバからの画像送信状況をMainに返します");
            listener.onSuccess(result);
        }

    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    interface Listener {
        void onSuccess(String result);
    }


}