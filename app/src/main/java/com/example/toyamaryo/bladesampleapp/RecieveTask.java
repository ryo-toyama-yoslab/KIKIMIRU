package com.example.toyamaryo.bladesampleapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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


public class RecieveTask extends AsyncTask<String, Void, String> {

    private Listener listener;
    private Activity mActivity;

    public RecieveTask(Activity activity){
        mActivity = activity;
    }

    // 非同期処理
    @Override
    protected String doInBackground(String... params) {

        // 使用するサーバーのURLに合わせる
        String urlSt = params[0];
        Log.d("URL：",urlSt);

        HttpsURLConnection httpConn = null;
        String result = null;
        //MainActiviyのexecute()の引数を増やせばparams[n]として使える
        //String word = "word="+params[0];
        StringBuilder sb = new StringBuilder();


        try{
            // 画像をjpeg形式でstreamに保存
            //ByteArrayOutputStream jpg = new ByteArrayOutputStream();
            //params[0].bmp.compress(Bitmap.CompressFormat.JPEG, 100, jpg);

            // URL設定
            URL url = new URL(urlSt);

            // HttpURLConnection
            httpConn = (HttpsURLConnection) url.openConnection();

            // request POST
            // httpConn.setRequestMethod("POST");

            // request GET
            httpConn.setRequestMethod("GET");

            // no Redirects
            httpConn.setInstanceFollowRedirects(false);

            // データを書き込む(書き込まない，GETの場合はfalseに)
            httpConn.setDoOutput(false);

            // 時間制限
            httpConn.setReadTimeout(10000);
            httpConn.setConnectTimeout(20000);

            //
            httpConn.setRequestProperty("Accept-Language", "jp");

            //レスポンスのボディ受信を許可(許可しないならfalseに)
            httpConn.setDoInput(true);
            try {
                //証明書情報　全て空を返す
                TrustManager[] tm = {new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] chain,
                                                   String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain,
                                                   String authType) throws CertificateException {
                    }
                }};
                SSLContext sslcontext = SSLContext.getInstance("SSL");
                sslcontext.init(null, tm, null);
                //ホスト名の検証ルール　何が来てもtrueを返す
                HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname,
                                          SSLSession session) {
                        return true;
                    }
                });


                //ここでsetSSLSocketFactoryを実行
                httpConn.setSSLSocketFactory(sslcontext.getSocketFactory());
            }catch(Exception e){

            }

            // 接続
            httpConn.connect();


            try {
                int resp = httpConn.getResponseCode();
                Log.d(TAG, "responseCode" + resp);
                InputStream is = httpConn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line = "";
                while ((line = reader.readLine()) != null)
                    sb.append(line);
                is.close();
                Log.d(TAG, "ReturnFromServer " + sb.toString());

            } catch (IOException e) {
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
        //結果としてどんな文字列が返ってきているかの確認用(使用時はsituation_infoの不可視設定を解除する必要有)
        //((TextView)mActivity.findViewById(R.id.situation_info)).setText(result);

        if (listener != null) {
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