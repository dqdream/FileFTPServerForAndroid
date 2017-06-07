package com.dq.fileftpserver;

import android.app.Application;

/**
 * Created by Administrator on 2017/6/7.
 */

public class FTPServerApp extends Application {

    private static FTPServerApp instance = null;

    public static FTPServerApp getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
