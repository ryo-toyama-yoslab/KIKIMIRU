package com.example.toyamaryo.bladesampleapp;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static android.content.ContentValues.TAG;

public class UploadTaskReadySSL extends AsyncTask<Param, Void, String> {

    private MainActivity mainActivity;

    public UploadTaskReadySSL(){
        mainActivity = new MainActivity();
    }

    // 非同期処理
    @Override
    protected String doInBackground(Param... params) {
        Param param = params[0];

        // 使用するサーバーのURLに合わせる
        String urlSt = param.uri;

        HttpsURLConnection httpsConn = null;
        StringBuilder sb = new StringBuilder();

        try{
            // URL設定
            URL url = new URL(urlSt);

            // HttpURLConnection
            httpsConn = (HttpsURLConnection) url.openConnection();

            // request POST
            httpsConn.setRequestMethod("POST");

            // no Redirects
            httpsConn.setInstanceFollowRedirects(false);

            // データを書き込む(書き込まない，GETの場合はfalseに)
            httpsConn.setDoOutput(true);

            // 時間制限
            httpsConn.setReadTimeout(10000000);
            httpsConn.setConnectTimeout(200000000);

            httpsConn.setRequestProperty("Accept-Language", "jp");

            //レスポンスのボディ受信を許可(許可しないならfalseに)
            httpsConn.setDoInput(true);

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
                httpsConn.setSSLSocketFactory(sslcontext.getSocketFactory());
            }catch(Exception e){

            }


            // 接続
            httpsConn.connect();


            try(// POSTデータ送信処理
                //Stringデータ送信パターン

                OutputStream outStream = httpsConn.getOutputStream()){

                InputStream is = httpsConn.getInputStream();
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
        } finally {
            /*
            if (httpConn != null) {
                httpConn.disconnect();
            }
            */
        }

        return sb.toString();
    }

    // 非同期処理が終了後、結果をメインスレッドに返す
    @Override
    protected void onPostExecute(String result) {
        //認識結果のラベル名をテキスト表示
        //super.onPostExecute(result);
        Log.d("SystemCheck", "不要な画像データを削除しました");
    }

}
