package com.example.toyamaryo.bladesampleapp;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class SaveLog extends Thread {

    Activity mainActivity;
    StringBuilder log; //ログ保存用

    SaveLog(Activity activity){
        this.mainActivity = activity;
    }

    @Override
    public void run(){
        Process process = null;
        BufferedReader bReader = null;

        final String pId = Integer.toString(android.os.Process.myPid());
        Log.i("debug", pId);

        try {
            process = new ProcessBuilder("logcat", "-v", "time").start();
            bReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            //取得する日時のフォーマットを指定
            DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
            Date date = new Date(System.currentTimeMillis());
            String fileName = "ApplicationLog_" + df.format(date) +".txt";
            String filePath = Environment.getExternalStorageDirectory().getPath() + "/" + fileName;
            Log.d("filePath",filePath);
            File file = new File(filePath);
            String line;
            do {
                line = bReader.readLine();
                // logが無い時は休む
                if (line.length() == 0) {
                    try {
                        sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                    continue;
                }

                if (line.contains(pId)) {
                    //ストレージに保存する
                    try{
                        FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                        OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream, "UTF-8");
                        BufferedWriter bw = new BufferedWriter(writer);
                        bw.write(line + "\r\n");
                        bw.flush();
                        bw.close();
                    } catch (Exception e) {
                        Log.d("Error",e.toString());
                    }

                }
            } while (true);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bReader != null) {
                try {
                    bReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
