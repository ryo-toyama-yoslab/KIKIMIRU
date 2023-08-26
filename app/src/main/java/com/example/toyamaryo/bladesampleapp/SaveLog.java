package com.example.toyamaryo.bladesampleapp;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class SaveLog extends Thread {

    StringBuilder log; //ログ保存用
    private boolean saveFlag;

    SaveLog(){
        saveFlag = true;
    }

    public void stopSaveLog(){
        Log.d("change Log Flag","ログ保存を停止します");
        saveFlag = false;
    }

    public void run(){
        Process process;
        FileOutputStream fileOutputStream = null;
        OutputStreamWriter out = null;
        BufferedReader bReader = null; // Logcat読み込み用
        BufferedWriter bw = null; // Log書き込み用
        File logFile;

        final String pId = Integer.toString(android.os.Process.myPid());
        Log.i("debug", pId);

        try {
            process = Runtime.getRuntime().exec(new String[] { "logcat", "-v", "time"});
            bReader = new BufferedReader(new InputStreamReader(process.getInputStream()), 1024);

            Locale japan = new Locale("ja","JP","JP");
            DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss", japan);
            Date date = new Date(System.currentTimeMillis());
            String fileName = "ApplicationLog_" + df.format(date) +".txt";

            String folderName = "KIKIMIRU_LOG";
            File logFolder = new File(Environment.getExternalStorageDirectory(), folderName);
            String logFilePath = logFolder.getPath() + "/" + fileName;
            logFile = new File(logFilePath); // ログ保存用テキストファイル作成

            String line;
            while(saveFlag){
                line = bReader.readLine();
                if(line.length() == 0){
                    try{
                        Thread.sleep(500);
                    }catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                if (line.contains(pId)) {
                    try{
                        fileOutputStream = new FileOutputStream(logFile, true); // 出力ストリームの生成(追記モード)
                        out = new OutputStreamWriter(fileOutputStream,"UTF-8");
                        bw = new BufferedWriter(out); // 出力ストリームのバッファリング
                        bw.write(line + "\r\n"); // テキストデータをバッファに格納
                        bw.flush(); // バッファのデータをファイルに書き込む
                    }catch(Exception e){
                        Log.d("LogSaveError",e.toString());
                    }finally{
                        if(fileOutputStream != null && out != null && bw != null) {
                            try{
                                fileOutputStream.close();
                                out.close();
                                bw.close();
                            }catch(IOException e){
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
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
