package com.shoo.demo.appwidgetanimation.appwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import com.shoo.demo.appwidgetanimation.MyApplication;
import com.shoo.demo.appwidgetanimation.R;
import com.shoo.demo.appwidgetanimation.data.BlockItem;
import com.shoo.demo.appwidgetanimation.util.Logger;

import java.util.List;

/**
 * Created by Shoo on 16-11-12.
 */
public class MyAppWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "MyAppWidgetProvider";

    public static final String APPWIDGET_REFRESH_ACTION = "com.shoo.demo.appwidget.REFRESH";
    public static final String ACTION_SHOW_PREV_PAGE = "com.shoo.demo.appwidget.SHOW_PREV_PAGE";
    public static final String ACTION_SHOW_NEXT_PAGE = "com.shoo.demo.appwidget.SHOW_NEXT_PAGE";

    private static final int ITEM_COUNT_PER_PAGE = 2;

    private static int sCurPageIndex;

    public MyAppWidgetProvider() {
        super();
        Logger.d(TAG, "MyAppWidgetProvider is created");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        Logger.d(TAG, "onReceive: action = " + action);
        switch (action) {
            case APPWIDGET_REFRESH_ACTION:
                if (AppWidgetDataManager.getInstance().refreshData()) {
                    showLoading();
                }
                break;
            case ACTION_SHOW_PREV_PAGE:
                showPrevPage(context);
                break;
            case ACTION_SHOW_NEXT_PAGE:
                showNextPage(context);
                break;
            default:
                break;
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Logger.d(TAG, "onUpdate");
        RemoteViews remoteViews = RemoteViewsBuilder.buildRootRemoteViews();
        // 点击刷新：刷新按钮
        PendingIntent refreshPendingIntent = getRefreshPendingIntent(context);
        remoteViews.setOnClickPendingIntent(R.id.wg_refresh_btn, refreshPendingIntent);
        // 空状态：加载失败/无网络
        remoteViews.setOnClickPendingIntent(R.id.wg_empty_view, refreshPendingIntent);
        // 切至上一页
        Intent prevPageIntent = new Intent(ACTION_SHOW_PREV_PAGE);
        remoteViews.setOnClickPendingIntent(R.id.iv_prev,
                PendingIntent.getBroadcast(context, 0, prevPageIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        // 切至下一页
        Intent nextPageIntent = new Intent(ACTION_SHOW_NEXT_PAGE);
        remoteViews.setOnClickPendingIntent(R.id.iv_next,
                PendingIntent.getBroadcast(context, 0, nextPageIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        AppWidgetUtil.publish(context, remoteViews);
        // 请求新数据
        AppWidgetDataManager.getInstance().refreshData();
        // 初始化
        setupAppWidget(context, remoteViews);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId,
                                          Bundle newOptions) {
        Logger.d(TAG, "onAppWidgetOptionsChanged ...");
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onEnabled(Context context) {
        Logger.d(TAG, "onEnabled ...");
        super.onEnabled(context);
        AppWidgetDataManager.getInstance().resetRefreshState();
    }

    /**
     * 所有桌面小部件被删除：清除定时刷新、关闭数据请求服务
     *
     * @param context
     */
    @Override
    public void onDisabled(Context context) {
        Logger.d(TAG, "onDisabled ...");
        super.onDisabled(context);
        AppWidgetDataManager.getInstance().resetRefreshState();
        AppWidgetDataManager.getInstance().stopDataService();
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Logger.d(TAG, "onDeleted ...");
        super.onDeleted(context, appWidgetIds);
    }

    /**
     * 刷新操作
     *
     * @param context
     * @return
     */
    public static PendingIntent getRefreshPendingIntent(Context context) {
        Intent refreshIntent = new Intent(APPWIDGET_REFRESH_ACTION);
        return PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * 设置桌面插件数据/状态
     *
     * @param context
     * @param remoteViews
     */
    private static void setupAppWidget(Context context, RemoteViews remoteViews) {
        setupListView(remoteViews);
        setupRefreshLayout(remoteViews);
        setupPageIndicator(remoteViews);
        AppWidgetUtil.partiallyUpdateAppWidget(context, remoteViews);
    }

    /**
     * 设置列表/空状态
     *
     * @param remoteViews
     */
    public static void setupListView(RemoteViews remoteViews) {
        Logger.d(TAG, "setupListView " + "");
        List<BlockItem> articleList = AppWidgetDataManager.getInstance().getArticleList();
        if (articleList != null && !articleList.isEmpty()) {
            showListView(remoteViews);
        } else {
            showEmptyView(remoteViews);
        }
    }

    /**
     * 显示列表
     *  @param remoteViews
     *
     */
    private static void showListView(RemoteViews remoteViews) {
        Logger.d(TAG, "showListView: curPageIndex = " + sCurPageIndex);
        remoteViews.setViewVisibility(R.id.wg_empty_view, View.GONE);
        remoteViews.setViewVisibility(R.id.wg_list_view, View.VISIBLE);
        remoteViews.removeAllViews(R.id.wg_list_view);
        int start = sCurPageIndex * ITEM_COUNT_PER_PAGE;
        int end = start + ITEM_COUNT_PER_PAGE;
        List<BlockItem> articleList = AppWidgetDataManager.getInstance().getArticleList();
        boolean hasValidData = checkIndexBounds(articleList, start, end);
        if (hasValidData) {
            remoteViews.addView(R.id.wg_list_view,
                    RemoteViewsBuilder.buildArticleListView(articleList, start, end, EAnimation.NONE));
        }
    }

    /**
     * 显示空状态：无数据/网络错误/加载中
     *
     * @param remoteViews
     */
    private static void showEmptyView(RemoteViews remoteViews) {
        Logger.d(TAG, "showEmptyView " + "");
        remoteViews.setViewVisibility(R.id.wg_list_view, View.GONE);
        remoteViews.setViewVisibility(R.id.wg_empty_view, View.VISIBLE);
        remoteViews.setTextViewText(R.id.wg_empty_view, AppWidgetUtil.getErrorMsg());
    }

    /**
     * 刷新按钮/动画状态
     *
     * @param remoteViews
     */
    private static void setupRefreshLayout(RemoteViews remoteViews) {
        boolean isRefreshing = AppWidgetDataManager.getInstance().isRefreshingData();
        remoteViews.setViewVisibility(R.id.wg_refresh_btn, isRefreshing ? View.GONE : View.VISIBLE);
        remoteViews.setViewVisibility(R.id.wg_loading_view, isRefreshing ? View.VISIBLE : View.GONE);
        Logger.d(TAG, "setupRefreshLayout: isRefreshing = " + isRefreshing);
    }

    /**
     * 页面指示器及页面切换按钮
     *
     * @param remoteViews
     */
    private static void setupPageIndicator(RemoteViews remoteViews) {
        List<BlockItem> articleList = AppWidgetDataManager.getInstance().getArticleList();
        int pageCnt = articleList != null ? articleList.size() / ITEM_COUNT_PER_PAGE : 0;
        int curIndex = sCurPageIndex;
        // 切页按钮状态
        boolean hasPrevPage = curIndex > 0;
        boolean hasNextPage = curIndex < pageCnt - 1;
        // 切页按钮初始状态不可点击，需要动态设置资源，处理按钮各状态显示效果
        remoteViews.setImageViewResource(R.id.iv_prev, R.drawable.wg_btn_prev);
        remoteViews.setImageViewResource(R.id.iv_next, R.drawable.wg_btn_next);
        remoteViews.setBoolean(R.id.iv_prev, "setEnabled", hasPrevPage);
        remoteViews.setBoolean(R.id.iv_next, "setEnabled", hasNextPage);
        // 当前选中页面索引
        String pageIndexStr = AppWidgetUtil.getPageIndexStr(curIndex, pageCnt);
        remoteViews.setTextViewText(R.id.tv_page_index, pageIndexStr);
        Logger.d(TAG, "setupPageIndicator: hasPrePage = " + hasNextPage + ", hasNextPage = " + hasNextPage);
    }

    /**
     * 更新数据
     */
    public static void notifyDataSetChanged() {
        Logger.d(TAG, "notifyDataSetChanged " + "");
        resetPageIndex();
        setupAppWidget(MyApplication.getAppContext(), RemoteViewsBuilder.buildRootRemoteViews());
    }

    /**
     * 重置选中页
     */
    private static void resetPageIndex() {
        sCurPageIndex = 0;
    }

    /**
     * 重置插件状态：取消刷新状态
     */
    public static void resetAppWidget() {
        Logger.d(TAG, "resetAppWidget " + "");
        AppWidgetDataManager.getInstance().resetRefreshState();
        setupAppWidget(MyApplication.getAppContext(), RemoteViewsBuilder.buildRootRemoteViews());
    }

    /**
     * 显示加载动画：刷新动画、刷新文本提示
     */
    public static void showLoading() {
        Logger.d(TAG, "showLoading " + "");
        RemoteViews remoteViews = RemoteViewsBuilder.buildRootRemoteViews();
        // 刷新动画
        remoteViews.setViewVisibility(R.id.wg_refresh_btn, View.GONE);
        remoteViews.setViewVisibility(R.id.wg_loading_view, View.VISIBLE);
        // 加载中文本提示
        List<BlockItem> articleList = AppWidgetDataManager.getInstance().getArticleList();
        if (articleList == null || articleList.isEmpty()) {
            // 显示加载状态
            remoteViews.setViewVisibility(R.id.wg_list_view, View.GONE);
            remoteViews.setViewVisibility(R.id.wg_empty_view, View.VISIBLE);
            remoteViews.setTextViewText(R.id.wg_empty_view, AppWidgetUtil.getLoadingStr());
        } else {
            // 显示列表
            remoteViews.setViewVisibility(R.id.wg_empty_view, View.GONE);
            remoteViews.setViewVisibility(R.id.wg_list_view, View.VISIBLE);
        }
        AppWidgetUtil.partiallyUpdateAppWidget(MyApplication.getAppContext(), remoteViews);
    }

    /**
     * 切换至下一页
     *
     * @param context
     */
    private static void showNextPage(Context context) {
        switchPage(context, sCurPageIndex, EAnimation.SLIDE_OUT_LEFT, sCurPageIndex + 1, EAnimation.SLIDE_IN_RIGHT);
    }

    /**
     * 切换至上一页
     *
     * @param context
     */
    private static void showPrevPage(Context context) {
        switchPage(context, sCurPageIndex, EAnimation.SLIDE_OUT_RIGHT, sCurPageIndex - 1, EAnimation.SLIDE_IN_LEFT);
    }

    public static void switchPage(Context context, int curPageIndex, EAnimation curPageAnim, int dstPageIndex,
                                  EAnimation dstPageAnim) {
        int dstStart = dstPageIndex * ITEM_COUNT_PER_PAGE;
        int dstEnd = dstStart + ITEM_COUNT_PER_PAGE;
        List<BlockItem> articleList = AppWidgetDataManager.getInstance().getArticleList();
        boolean hasNextPage = checkIndexBounds(articleList, dstStart, dstEnd);
        if (hasNextPage) {
            RemoteViews remoteViews = RemoteViewsBuilder.buildRootRemoteViews();
            remoteViews.removeAllViews(R.id.wg_list_view);

            int curStart = curPageIndex * ITEM_COUNT_PER_PAGE;
            int curEnd = curStart + ITEM_COUNT_PER_PAGE;
            boolean hasCurPage = checkIndexBounds(articleList, curStart, curEnd);
            if (hasCurPage) {
                // 当前选中页
                remoteViews.addView(R.id.wg_list_view, RemoteViewsBuilder.buildArticleListView
                        (articleList, curStart, curEnd, curPageAnim));
                // 目标页
                remoteViews.addView(R.id.wg_list_view, RemoteViewsBuilder.buildArticleListView(
                        articleList, dstStart, dstEnd, dstPageAnim));
            } else {
                // 直接显示目标页
                remoteViews.addView(R.id.wg_list_view, RemoteViewsBuilder.buildArticleListView(
                        articleList, dstStart, dstEnd, EAnimation.NONE));
            }
            // 设置选中页
            sCurPageIndex = dstPageIndex;
            // 页面指示器
            setupPageIndicator(remoteViews);

            AppWidgetUtil.partiallyUpdateAppWidget(context, remoteViews);
        }
    }

    private static boolean checkIndexBounds(List<BlockItem> articleList, int start, int end) {
        return articleList != null && start >= 0 && start < end && articleList.size() >= end;
    }
}
