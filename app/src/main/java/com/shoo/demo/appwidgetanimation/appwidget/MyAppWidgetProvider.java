package com.shoo.demo.appwidgetanimation.appwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/**
 * 桌面插件消息入口
 *
 * Created by Shoo on 16-11-12.
 */

public class MyAppWidgetProvider extends AppWidgetProvider {

    public MyAppWidgetProvider() {
        super();
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        // preview
        RemoteViewsManager.getInstance().prepareAppWidget();
        // refresh data
        AppWidgetManagerService.scheduleRefreshList(context);
    }

    /**
     * 所有桌面小部件被删除：清除定时刷新、关闭数据请求服务
     *
     * @param context
     */
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        AppWidgetManagerService.stopService(context);
    }
}
