package com.shoo.demo.appwidgetanimation.appwidget;

import android.app.Service;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.shoo.demo.appwidgetanimation.data.BlockItem;
import com.shoo.demo.appwidgetanimation.util.Logger;
import com.shoo.demo.appwidgetanimation.util.RxUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by Shoo on 16-11-12.
 */
public class AppWidgetDataService extends Service {

    private static final String TAG = "AppWidgetDataService";
    private Subscription mSubscription;

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d(TAG, "onCreate ... ");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.d(TAG, "onStartCommand ... ");
        mSubscription = Observable
                .timer(2, TimeUnit.SECONDS)
                .map(new Func1<Long, List<BlockItem>>() {
                    @Override
                    public List<BlockItem> call(Long aLong) {
                        List<BlockItem> list = new ArrayList<>();
                        for (int i = 0; i < 10; i++) {
                            list.add(new BlockItem("item " + (i + 1)));
                        }
                        return list;
                    }
                })
                .observeOn(Schedulers.io())
                .subscribe(AppWidgetDataManager.getInstance().getDataObserver());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.d(TAG, "onDestroy ... ");
        unsubscribe();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        // 系统即将杀掉当前
        if (ComponentCallbacks2.TRIM_MEMORY_COMPLETE == level) {
            MyAppWidgetProvider.resetAppWidget();
            unsubscribe();
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // 当前服务将被强制关闭
        MyAppWidgetProvider.resetAppWidget();
        unsubscribe();
    }

    private void unsubscribe() {
        RxUtils.unsubscribe(mSubscription);
    }
}
