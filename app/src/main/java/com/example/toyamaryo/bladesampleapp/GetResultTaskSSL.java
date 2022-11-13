package com.example.toyamaryo.bladesampleapp;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class GetResultTaskSSL extends AsyncTask<Param, Void, String>  {

    private GetResultTaskSSL.Listener listener;
    private MainActivity mainActivity;

    public GetResultTaskSSL(){
        mainActivity = new MainActivity();
    }


    // 非同期処理
    @Override
    protected String doInBackground(Param... params) {
        Param param = params[0];

        // 使用するサーバーのURLに合わせる
        String urlSt = param.uri;

        HttpsURLConnection httpConn = null;
        StringBuilder sb = new StringBuilder();

        try {
            // URL設定
            URL url = new URL(urlSt);

            // HttpURLConnection
            httpConn = (HttpsURLConnection) url.openConnection();

            // request POST
            httpConn.setRequestMethod("GET");

            // no Redirects
            httpConn.setInstanceFollowRedirects(false);

            // データを書き込む(書き込まない，GETの場合はfalseに)
            httpConn.setDoOutput(false);

            // 時間制限
            httpConn.setReadTimeout(10000000);
            httpConn.setConnectTimeout(200000000);

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
            } catch (Exception e) {
                e.printStackTrace();
            }

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
        //Log.d("ReturnFromServer : ", sb.toString());
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

    void setListener(GetResultTaskSSL.Listener listener) {
        this.listener = listener;
    }

    void removeListener(GetResultTaskSSL.Listener listener){
        this.listener = null;
    }

    interface Listener {
        void onSuccess(String result);
    }
}
