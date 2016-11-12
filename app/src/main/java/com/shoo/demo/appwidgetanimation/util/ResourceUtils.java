package com.shoo.demo.appwidgetanimation.util;

import android.content.Context;

/**
 * Created by Shoo on 16-11-12.
 */

public class ResourceUtils {

    private static Context sContext;

    public static void initContext(Context context) {
        sContext = context;
    }

    public static String getString(int resId) {
        return sContext.getString(resId);
    }

    public static String getPackageName() {
        return sContext.getPackageName();
    }
}
