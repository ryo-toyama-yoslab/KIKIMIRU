package com.example.toyamaryo.bladesampleapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
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


public class UploadTask extends AsyncTask<Param, Void, String> {

    private Listener listener;
    private Activity mActivity;
    private MainActivity mainActivity;
    private Bitmap decodedByte;
    private int picture_count;

    public UploadTask(Activity activity){
        mActivity = activity;
        ((TextView)mActivity.findViewById(R.id.return_text)).setText("認識中だよ(^ - ~)");

        mainActivity = new MainActivity();
    }

    // 非同期処理
    @Override
    protected String doInBackground(Param... params) {
        Param param = params[0];

        // 使用するサーバーのURLに合わせる
        String urlSt = param.uri;
        String img = "img=";
        String result = null;
        //Log.d("URL：",urlSt);

        HttpsURLConnection httpConn = null;
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
            httpConn = (HttpsURLConnection) url.openConnection();

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
                result = "POST送信エラー";
            }

            /*
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

            */

           /* final int status = httpConn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                // レスポンスを受け取る処理等
                result="HTTP_OK";
            }
            else{
                result="status="+String.valueOf(status);
        }*/

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
        //((TextView)mActivity.findViewById(R.id.return_text)).setText(result);

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