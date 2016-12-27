package com.shoo.demo.appwidgetanimation.appwidget;

import android.app.PendingIntent;
import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;

import com.shoo.demo.appwidgetanimation.MyApplication;
import com.shoo.demo.appwidgetanimation.R;
import com.shoo.demo.appwidgetanimation.data.BlockItem;
import com.shoo.demo.appwidgetanimation.util.ResourceUtils;
import com.shoo.demo.appwidgetanimation.util.RxUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Subscription;
import rx.functions.Action0;

/**
 * 管理桌面插件各模块的展示
 *
 * Created by Shoo on 16-12-1.
 */
public class RemoteViewsManager {

    private Subscription mRemoveViewSubscription;

    private static class Holder {
        public static final RemoteViewsManager INSTANCE = new RemoteViewsManager();
    }

    private static final int ITEM_COUNT_PER_PAGE = 2;

    private static RemoteViewsManager sInstance;

    private final Context mContext;

    public static RemoteViewsManager getInstance() {
        if (sInstance == null) {
            sInstance = Holder.INSTANCE;
        }
        return sInstance;
    }

    private RemoteViewsManager() {
        mContext = MyApplication.getAppContext();
    }

    public void prepareAppWidget() {
        RemoteViews remoteViews = RemoteViewsBuilder.buildRootRemoteViews();
        // 点击刷新：刷新按钮
        PendingIntent refreshPendingIntent = AppWidgetManagerService.getRefreshPendingIntent(mContext);
        remoteViews.setOnClickPendingIntent(R.id.wg_refresh_btn, refreshPendingIntent);
        // 空状态：加载失败/无网络
        remoteViews.setOnClickPendingIntent(R.id.wg_empty_view, refreshPendingIntent);
        // 切页按钮点击事件
        setupSwitchPagePendingIntent(remoteViews, 0);
        AppWidgetUtil.publish(mContext, remoteViews);
    }

    private void setupSwitchPagePendingIntent(RemoteViews remoteViews, int curPageIndex) {
        // 切至上一页
        PendingIntent showPrevPendingIntent = AppWidgetManagerService.getShowPrevPendingIntent(mContext, curPageIndex);
        remoteViews.setOnClickPendingIntent(R.id.iv_prev, showPrevPendingIntent);
        // 切至下一页
        PendingIntent showNextPendingIntent = AppWidgetManagerService.getShowNextPendingIntent(mContext, curPageIndex);
        remoteViews.setOnClickPendingIntent(R.id.iv_next, showNextPendingIntent);
    }

    public void onLoadingStart() {
        RemoteViews remoteViews = RemoteViewsBuilder.buildRootRemoteViews();
        showLoading(remoteViews);
        AppWidgetUtil.partiallyUpdateAppWidget(mContext, remoteViews);
    }

    private void showLoading(RemoteViews remoteViews) {
        remoteViews.setViewVisibility(R.id.wg_refresh_btn, View.GONE);
        remoteViews.setViewVisibility(R.id.wg_loading_view, View.VISIBLE);
        remoteViews.setTextViewText(R.id.wg_empty_view, AppWidgetUtil.getLoadingStr());
    }

    public void resetAppWidget(List<BlockItem> list) {
        RemoteViews remoteViews = RemoteViewsBuilder.buildRootRemoteViews();
        showListView(remoteViews, list, 0);
        setupPageIndicator(remoteViews, list, 0);
        hideLoading(remoteViews);
        AppWidgetUtil.partiallyUpdateAppWidget(mContext, remoteViews);
    }

    /**
     * 显示数据
     *
     * @param remoteViews
     * @param list
     * @param pageIndex
     */
    private void showListView(RemoteViews remoteViews, List<BlockItem> list, int pageIndex) {
        remoteViews.setViewVisibility(R.id.wg_empty_view, View.GONE);
        remoteViews.setViewVisibility(R.id.wg_list_view, View.VISIBLE);
        remoteViews.removeAllViews(R.id.wg_list_view);
        int start = pageIndex * ITEM_COUNT_PER_PAGE;
        int end = start + ITEM_COUNT_PER_PAGE;
        boolean hasValidData = checkIndexBounds(list, start, end);
        if (hasValidData) {
            RxUtils.unsubscribe(mRemoveViewSubscription);
            remoteViews.addView(R.id.wg_list_view,
                    RemoteViewsBuilder.buildListView(list, start, end, EAnimation.NONE));
        }
    }

    /**
     * 页面指示器及页面切换按钮
     */
    private void setupPageIndicator(RemoteViews remoteViews, List<BlockItem> list, int curPageIndex) {
        int pageCnt = list != null ? list.size() / ITEM_COUNT_PER_PAGE : 0;
        // 切页按钮状态
        boolean hasPrevPage = curPageIndex > 0;
        boolean hasNextPage = curPageIndex < pageCnt - 1;
        // 切页按钮初始状态不可点击，需要动态设置资源，处理按钮各状态显示效果
        remoteViews.setImageViewResource(R.id.iv_prev, R.drawable.wg_btn_prev);
        remoteViews.setImageViewResource(R.id.iv_next, R.drawable.wg_btn_next);
        remoteViews.setBoolean(R.id.iv_prev, "setEnabled", hasPrevPage);
        remoteViews.setBoolean(R.id.iv_next, "setEnabled", hasNextPage);
        // 点击事件
        setupSwitchPagePendingIntent(remoteViews, curPageIndex);
        // 当前选中页面索引
        String pageIndexStr = AppWidgetUtil.getPageIndexStr(curPageIndex, pageCnt);
        remoteViews.setTextViewText(R.id.tv_page_index, pageIndexStr);
    }

    public void showEmptyView() {
        RemoteViews remoteViews = RemoteViewsBuilder.buildRootRemoteViews();
        remoteViews.setViewVisibility(R.id.wg_empty_view, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.wg_list_view, View.GONE);
        setupPageIndicator(remoteViews, null, 0);
        AppWidgetUtil.partiallyUpdateAppWidget(mContext, remoteViews);
    }

    public void onLoadingStop() {
        RemoteViews remoteViews = RemoteViewsBuilder.buildRootRemoteViews();
        hideLoading(remoteViews);
        AppWidgetUtil.partiallyUpdateAppWidget(mContext, remoteViews);
    }

    /**
     * 刷新按钮/动画状态
     *
     * @param remoteViews
     */
    private void hideLoading(RemoteViews remoteViews) {
        remoteViews.setViewVisibility(R.id.wg_refresh_btn, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.wg_loading_view, View.GONE);
        remoteViews.setTextViewText(R.id.wg_empty_view, AppWidgetUtil.getErrorMsg());
    }

    /**
     * 切换至下一页
     *
     * @param list
     * @param curPageIndex
     */
    public void showNextPage(List<BlockItem> list, int curPageIndex) {
        switchPage(list, curPageIndex, EAnimation.SLIDE_OUT_LEFT, curPageIndex + 1, EAnimation.SLIDE_IN_RIGHT);
    }

    /**
     * 切换至上一页
     *
     * @param list
     * @param curPageIndex
     */
    public void showPrevPage(List<BlockItem> list, int curPageIndex) {
        switchPage(list, curPageIndex, EAnimation.SLIDE_OUT_RIGHT, curPageIndex - 1, EAnimation.SLIDE_IN_LEFT);
    }

    public void switchPage(List<BlockItem> list, int curPageIndex, EAnimation curPageAnim, int dstPageIndex,
                           EAnimation dstPageAnim) {
        int dstStart = dstPageIndex * ITEM_COUNT_PER_PAGE;
        int dstEnd = dstStart + ITEM_COUNT_PER_PAGE;
        boolean hasNextPage = checkIndexBounds(list, dstStart, dstEnd);
        if (hasNextPage) {
            RemoteViews remoteViews = RemoteViewsBuilder.buildRootRemoteViews();
            remoteViews.removeAllViews(R.id.wg_list_view);

            int curStart = curPageIndex * ITEM_COUNT_PER_PAGE;
            int curEnd = curStart + ITEM_COUNT_PER_PAGE;
            boolean hasCurPage = checkIndexBounds(list, curStart, curEnd);
            if (hasCurPage) {
                // 当前选中页
                remoteViews.addView(R.id.wg_list_view, RemoteViewsBuilder.buildListView(
                        list, curStart, curEnd, curPageAnim));
                // 下一页
                remoteViews.addView(R.id.wg_list_view, RemoteViewsBuilder.buildListView(
                        list, dstStart, dstEnd, dstPageAnim));
            } else {
                remoteViews.addView(R.id.wg_list_view, RemoteViewsBuilder.buildListView(
                        list, dstStart, dstEnd, EAnimation.NONE));
            }
            // 页面指示器
            setupPageIndicator(remoteViews, list, dstPageIndex);

            AppWidgetUtil.partiallyUpdateAppWidget(mContext, remoteViews);

            // 切页动画执行结束后，需要移除用于执行动画的页面；否则，将插件移动至其他屏幕页面后，会出现页面重叠的问题
            removeTmpAnimView(list, dstStart, dstEnd);
        } else {
            // 重新加载数据
            RemoteViews remoteViews = RemoteViewsBuilder.buildRootRemoteViews();
            showLoading(remoteViews);
            remoteViews.setViewVisibility(R.id.wg_empty_view, View.GONE);
            remoteViews.setViewVisibility(R.id.wg_list_view, View.VISIBLE);
            AppWidgetManagerService.scheduleRefreshList(mContext);
        }
    }

    /**
     * 移除用于执行动画的页面，以避免将插件移动至其他屏幕页面后，出现页面重叠的问题
     *
     * @param list
     * @param dstStart
     * @param dstEnd
     */
    private void removeTmpAnimView(final List<BlockItem> list, final int dstStart, final int dstEnd) {
        RxUtils.unsubscribe(mRemoveViewSubscription);
        mRemoveViewSubscription = RxUtils.scheduleOnCurThread(new Action0() {

            @Override
            public void call() {
                RemoteViews remoteViews = RemoteViewsBuilder.buildRootRemoteViews();
                remoteViews.removeAllViews(R.id.wg_list_view);
                remoteViews.addView(R.id.wg_list_view, RemoteViewsBuilder.buildListView(
                        list, dstStart, dstEnd, EAnimation.NONE));
                AppWidgetUtil.partiallyUpdateAppWidget(mContext, remoteViews);
            }
        }, ResourceUtils.getInteger(R.integer.wg_duration_transition), TimeUnit.MILLISECONDS);
    }

    private boolean checkIndexBounds(List<BlockItem> list, int start, int end) {
        return list != null && start >= 0 && start < end && list.size() >= end;
    }

}
