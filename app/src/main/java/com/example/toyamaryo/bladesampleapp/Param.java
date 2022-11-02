package com.example.toyamaryo.bladesampleapp;

import android.graphics.Bitmap;

class Param {
    public String uri;
    public Bitmap bmp;
    public String str;

    public Param(String uri, Bitmap bmp) {
        this.uri = uri;
        this.bmp = bmp;
    }


    public Param(String uri) {
        this.uri = uri;
    }

    public Param(String uri, String str) {
        this.uri = uri;
        this.str = str;
    }
}