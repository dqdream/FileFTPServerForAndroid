package com.dq.fileftpserver;

import android.util.Log;

public class LogUtil {
    public static String TAG = "Ftp";
    private static final boolean LOG_DEBUG = true;
    private static final boolean LOG_INFO = true;
    private static final boolean LOG_ERROR = true;

    public static void d(String msg) {
        d(TAG, msg);
    }

    public static void d(String tag, String msg) {
        if (LOG_DEBUG) {
            Log.d(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (LOG_INFO) {
            Log.i(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (LOG_ERROR) {
            Log.e(tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (LOG_ERROR) {
            Log.e(tag, msg, tr);
        }
    }
}
