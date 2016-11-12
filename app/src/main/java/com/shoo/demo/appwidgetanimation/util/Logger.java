package com.shoo.demo.appwidgetanimation.util;

import android.util.Log;

import com.shoo.demo.appwidgetanimation.BuildConfig;

/**
 * Created by Shoo on 16-11-12.
 */

public class Logger {

    public static final boolean DEBUG = BuildConfig.DEBUG;

    public static void d(String tag, String msg) {
        if (DEBUG) {
            Log.d(tag, msg);
        }
    }

}
