package com.shoo.demo.appwidgetanimation;

import android.app.Application;
import android.content.Context;

import com.shoo.demo.appwidgetanimation.util.ResourceUtils;

/**
 * Created by Shoo on 16-11-12.
 */

public class MyApplication extends Application {

    private static Context sAppContext;

    public static Context getAppContext() {
        return sAppContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sAppContext = getApplicationContext();
        ResourceUtils.initContext(sAppContext);
    }
}
