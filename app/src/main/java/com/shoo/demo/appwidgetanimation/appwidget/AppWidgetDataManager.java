package com.shoo.demo.appwidgetanimation.appwidget;

import android.content.Context;
import android.content.Intent;

import com.shoo.demo.appwidgetanimation.MyApplication;
import com.shoo.demo.appwidgetanimation.R;
import com.shoo.demo.appwidgetanimation.data.BlockItem;
import com.shoo.demo.appwidgetanimation.util.Logger;
import com.shoo.demo.appwidgetanimation.util.RxUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;

/**
 * Created by Shoo on 16-11-12.
 */

public class AppWidgetDataManager {

    private static final String TAG = "AppWidgetDataManager";

    private static AppWidgetDataManager sInstance;

    private final Context mContext;
    private boolean mIsRefreshingData;
    private long mLastUpdateTime;
    private List<BlockItem> mArticleList;
    private Observer<List<BlockItem>> mDataObserver;

    public synchronized static AppWidgetDataManager getInstance() {
        if (sInstance == null) {
            sInstance = new AppWidgetDataManager();
        }
        return sInstance;
    }

    private AppWidgetDataManager() {
        mContext = MyApplication.getAppContext();
    }

    /**
     * 更新数据
     *
     * @return
     */
    public boolean refreshData() {
        Logger.d(TAG, "refreshData: isRefreshingData = " + mIsRefreshingData);
        boolean isRefreshingData = mIsRefreshingData;
        if (!isRefreshingData) {
            mIsRefreshingData = true;
            setupStartTime();

            Intent intent = new Intent(mContext, AppWidgetDataService.class);
            mContext.startService(intent);
        }
        return !isRefreshingData;
    }

    /**
     * 设置加载开始时间：用于处理完整的动画加载
     */
    private void setupStartTime() {
        mLastUpdateTime = System.currentTimeMillis();
    }

    /**
     * 完整动画加载剩余时间
     *
     * @return
     */
    private long getLoadingTimeLeft() {
        long curTimeMillis = System.currentTimeMillis();
        long loadingDuration = (long) mContext.getResources().getInteger(R.integer.wg_duration_loading_view);
        long timeLeft = loadingDuration - (curTimeMillis - mLastUpdateTime) % loadingDuration;
        Logger.d(TAG, "getLoadingTimeLeft: timeLeft = " + timeLeft);
        return timeLeft;
    }

    /**
     * 保存数据
     *
     * @param data
     */
    private void saveData(List<BlockItem> data) {
        mArticleList = data;
    }

    /**
     * 重置刷新状态、加载开始时间
     */
    public void resetRefreshState() {
        mIsRefreshingData = false;
        mLastUpdateTime = 0;
    }

    /**
     * 停止数据请求服务
     */
    public void stopDataService() {
        Intent intent = new Intent(mContext, AppWidgetDataService.class);
        mContext.stopService(intent);
    }

    /**
     * 观察数据请求的返回
     *
     * @return
     */
    public Observer<List<BlockItem>> getDataObserver() {
        if (mDataObserver == null) {
            mDataObserver = new DataObserver();
        }
        return mDataObserver;
    }

    /**
     * 获取列表
     *
     * @return
     */
    public List<BlockItem> getArticleList() {
        return mArticleList;
    }

    /**
     * 是否正在加载数据
     *
     * @return
     */
    public boolean isRefreshingData() {
        return mIsRefreshingData;
    }

    private class DataObserver implements Observer<List<BlockItem>> {

        private Subscription mUpdateSubscription;

        @Override
        public void onNext(List<BlockItem> dataHolder) {
            setupAppWidget(dataHolder);
        }

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable throwable) {
            setupAppWidget(null);
        }

        private void setupAppWidget(final List<BlockItem> data) {
            Logger.d(TAG, "setupAppWidget() called with: data = [" + data + "]");
            RxUtils.unsubscribe(mUpdateSubscription);
            // 延迟执行数据更新，以确保加载动画的完整显示
            mUpdateSubscription = RxUtils.scheduleOnMainThread(new Action0() {
                @Override
                public void call() {
                    // 保存数据
                    if (data != null) {
                        saveData(data);
                    }
                    // 更新数据加载状态
                    mIsRefreshingData = false;
                    // 更新数据
                    MyAppWidgetProvider.notifyDataSetChanged();
                }
            }, getLoadingTimeLeft(), TimeUnit.MILLISECONDS);
        }
    }
}
