package com.shoo.demo.appwidgetanimation.appwidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

import com.shoo.demo.appwidgetanimation.R;
import com.shoo.demo.appwidgetanimation.util.ResourceUtils;

/**
 * Created by Shoo on 16-11-12.
 */
public class AppWidgetUtil {

    /**
     * 当前选中页面索引
     *
     * @return
     * @param pageCnt
     */
    public static String getPageIndexStr(int curIndex, int pageCnt) {
        return pageCnt > 0 ? (curIndex + 1) + "/" + pageCnt : "--";
    }

    /**
     * 获取错误提示
     *
     * @return
     */
    public static String getErrorMsg() {
        if (AppWidgetDataManager.getInstance().isRefreshingData()) {
            return getLoadingStr();
        } else if (!isNetworkAvailable()) {
            return getNoNetworkString();
        } else {
            return getNoNewsString();
        }
    }

    /**
     * 是否联网
     *
     * @return
     */
    private static boolean isNetworkAvailable() {
        // something to be done here to check network connection
        return true;
    }

    /**
     * 加载数据提示
     *
     * @return
     */
    public static String getLoadingStr() {
        return ResourceUtils.getString(R.string.wg_loading_please_wait);
    }

    /**
     * 加载失败提示
     *
     * @return
     */
    public static String getNoNewsString() {
        return ResourceUtils.getString(R.string.wg_no_news_loaded);
    }

    /**
     * 无网络提示
     *
     * @return
     */
    public static String getNoNetworkString() {
        return ResourceUtils.getString(R.string.wg_no_network_connected);
    }

    /**
     * 更新桌面插件
     *
     * @param context
     * @param remoteViews
     */
    public static void publish(Context context, RemoteViews remoteViews) {
        getAppWidgetManager(context).updateAppWidget(getAppWidgetIds(context), remoteViews);
    }

    /**
     * 部分更新桌面插件修改
     *
     * @param context
     * @param remoteViews
     */
    public static void partiallyUpdateAppWidget(Context context, RemoteViews remoteViews) {
        getAppWidgetManager(context).partiallyUpdateAppWidget(getAppWidgetIds(context), remoteViews);
    }

    private static AppWidgetManager getAppWidgetManager(Context context) {
        return AppWidgetManager.getInstance(context);
    }

    private static int[] getAppWidgetIds(Context context) {
        return getAppWidgetManager(context).getAppWidgetIds(new ComponentName(context, MyAppWidgetProvider
                .class));
    }
}
