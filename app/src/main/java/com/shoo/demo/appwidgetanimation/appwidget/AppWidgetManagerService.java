package com.shoo.demo.appwidgetanimation.appwidget;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.shoo.demo.appwidgetanimation.R;
import com.shoo.demo.appwidgetanimation.data.BlockItem;
import com.shoo.demo.appwidgetanimation.helper.DefaultSubscriber;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * 管理桌面插件各模块点击事件
 *
 * Created by Shoo on 16-12-1.
 */
public class AppWidgetManagerService extends Service {

    private static final String KEY_APP_WIDGET_ACTION = "app_widget_action";
    private static final String KEY_CUR_PAGE_INDEX = "cur_page_index";
    private static final String KEY_IS_REFRESH_ACTION = "is_refresh_action";
    private static final String KEY_ITEM_POS = "item_pos";

    private static final int ACTION_REFRESH_LIST = 0x100;
    private static final int ACTION_CLICK_ITEM = 0x200;
    private static final int ACTION_SHOW_NEXT_PAGE = 0x300;
    private static final int ACTION_SHOW_PREV_PAGE = 0x400;

    private Handler mHandler;
    private Looper mLooper;
    private boolean mIsRefreshing;
    private long mRefreshStartTime;

    @Override
    public void onCreate() {
        super.onCreate();
        // prepare thread
        HandlerThread handlerThread = new HandlerThread("AppWidgetManagerThread");
        handlerThread.start();
        // prepare handler
        mLooper = handlerThread.getLooper();
        mHandler = new ActionHandler(mLooper, this);
        // init
        mIsRefreshing = false;
        mRefreshStartTime = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int action = intent.getIntExtra(KEY_APP_WIDGET_ACTION, -1);
            if (action >= 0) {
                Message msg = Message.obtain();
                msg.what = action;
                msg.obj = intent;
                if (ACTION_SHOW_NEXT_PAGE == action || ACTION_SHOW_PREV_PAGE == action) {
                    mHandler.removeMessages(ACTION_SHOW_NEXT_PAGE);
                    mHandler.removeMessages(ACTION_SHOW_PREV_PAGE);
                }
                mHandler.sendMessage(msg);
            }
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLooper.quit();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (ComponentCallbacks2.TRIM_MEMORY_COMPLETE == level) {
            mIsRefreshing = false;
            RemoteViewsManager.getInstance().onLoadingStop();
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        mIsRefreshing = false;
        RemoteViewsManager.getInstance().onLoadingStop();
    }

    public static void scheduleRefreshList(Context context) {
        Intent intent = new Intent(context, AppWidgetManagerService.class);
        intent.putExtra(KEY_APP_WIDGET_ACTION, ACTION_REFRESH_LIST);
        context.startService(intent);
    }

    public static PendingIntent getRefreshPendingIntent(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_IS_REFRESH_ACTION, true);
        return createPendingIntent(context, ACTION_REFRESH_LIST, bundle);
    }

    public static PendingIntent getItemClickPendingIntent(Context context, int pos) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_ITEM_POS, pos);
        return createPendingIntent(context, ACTION_CLICK_ITEM, bundle);
    }

    public static PendingIntent getShowNextPendingIntent(Context context, int curPageIndex) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_CUR_PAGE_INDEX, curPageIndex);
        return createPendingIntent(context, ACTION_SHOW_NEXT_PAGE, bundle);
    }

    public static PendingIntent getShowPrevPendingIntent(Context context, int curPageIndex) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_CUR_PAGE_INDEX, curPageIndex);
        return createPendingIntent(context, ACTION_SHOW_PREV_PAGE, bundle);
    }

    private static PendingIntent createPendingIntent(Context context, int action, Bundle bundle) {
        Intent intent = new Intent(context, AppWidgetManagerService.class);
        intent.putExtra(KEY_APP_WIDGET_ACTION, action);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        return PendingIntent.getService(context, action, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void refreshList() {
        if (!mIsRefreshing) {
            setupStartTime();
            RemoteViewsManager.getInstance().onLoadingStart();
            AppWidgetDataLoader.getInstance().start()
                    .flatMap(new Func1<List<BlockItem>, Observable<List<BlockItem>>>() {
                        @Override
                        public Observable<List<BlockItem>> call(final List<BlockItem> blockItems) {
                            // 等加载动画回到初始位置后，再执行数据的更新
                            return Observable
                                    .timer(getLoadingTimeLeft(), TimeUnit.MILLISECONDS)
                                    .flatMap(new Func1<Long, Observable<List<BlockItem>>>() {
                                        @Override
                                        public Observable<List<BlockItem>> call(Long aLong) {
                                            return Observable.just(blockItems);
                                        }
                                    });
                        }
                    })
                    .onErrorResumeNext(new Func1<Throwable, Observable<? extends List<BlockItem>>>() {
                        @Override
                        public Observable<? extends List<BlockItem>> call(final Throwable throwable) {
                            // 等加载动画回到初始位置后，再停止加载动画
                            return Observable
                                    .timer(getLoadingTimeLeft(), TimeUnit.MILLISECONDS)
                                    .flatMap(new Func1<Long, Observable<List<BlockItem>>>() {
                                        @Override
                                        public Observable<List<BlockItem>> call(Long aLong) {
                                            return Observable.error(throwable);
                                        }
                                    });
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new DefaultSubscriber<List<BlockItem>>() {
                        @Override
                        public void onNext(final List<BlockItem> blockItems) {
                            super.onNext(blockItems);
                            mIsRefreshing = false;
                            if (blockItems != null && !blockItems.isEmpty()) {
                                AppWidgetDataLoader.getInstance().saveData(blockItems);
                                RemoteViewsManager.getInstance().resetAppWidget(blockItems);
                            } else {
                                RemoteViewsManager.getInstance().showEmptyView();
                                RemoteViewsManager.getInstance().onLoadingStop();
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            super.onError(e);
                            mIsRefreshing = false;
                            RemoteViewsManager.getInstance().onLoadingStop();
                        }
                    });
            mIsRefreshing = true;
        }
    }

    /**
     * 设置加载开始时间：用于处理完整的动画加载
     */
    private void setupStartTime() {
        mRefreshStartTime = System.currentTimeMillis();
    }

    /**
     * 完整动画加载剩余时间
     *
     * @return
     */
    private long getLoadingTimeLeft() {
        long curTimeMillis = System.currentTimeMillis();
        long loadingDuration = (long) getResources().getInteger(R.integer.wg_duration_loading_view);
        return loadingDuration - (curTimeMillis - mRefreshStartTime) % loadingDuration;
    }

    private void onItemClick(Intent intent) {
        int pos = intent.getIntExtra(KEY_ITEM_POS, 0);
        Toast.makeText(this, "item " + (pos + 1) + " is clicked", Toast.LENGTH_SHORT).show();
    }

    private void showNextPage(Intent intent) {
        final int curPageIndex = intent.getIntExtra(KEY_CUR_PAGE_INDEX, 0);
        AppWidgetDataLoader.getInstance().getDataObservable()
                .subscribe(new DefaultSubscriber<List<BlockItem>>() {
                    @Override
                    public void onNext(List<BlockItem> blockItems) {
                        super.onNext(blockItems);
                        RemoteViewsManager.getInstance().showNextPage(blockItems, curPageIndex);
                    }
                });
    }

    private void showPrevPage(Intent intent) {
        final int curPageIndex = intent.getIntExtra(KEY_CUR_PAGE_INDEX, 0);
        AppWidgetDataLoader.getInstance().getDataObservable()
                .subscribe(new DefaultSubscriber<List<BlockItem>>() {
                    @Override
                    public void onNext(List<BlockItem> blockItems) {
                        super.onNext(blockItems);
                        RemoteViewsManager.getInstance().showPrevPage(blockItems, curPageIndex);
                    }
                });
    }

    public static void stopService(Context context) {
        Intent intent = new Intent(context, AppWidgetManagerService.class);
        context.stopService(intent);
    }

    private static class ActionHandler extends Handler {

        private WeakReference<AppWidgetManagerService> mServiceRef;

        public ActionHandler(Looper looper, AppWidgetManagerService service) {
            super(looper);
            mServiceRef = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mServiceRef.get() == null) {
                return;
            }

            AppWidgetManagerService service = mServiceRef.get();
            int what = msg.what;
            switch (what) {
                case ACTION_REFRESH_LIST:
                    service.refreshList();
                    break;
                case ACTION_CLICK_ITEM:
                    service.onItemClick((Intent) msg.obj);
                    break;
                case ACTION_SHOW_NEXT_PAGE:
                    service.showNextPage(((Intent) msg.obj));
                    break;
                case ACTION_SHOW_PREV_PAGE:
                    service.showPrevPage(((Intent) msg.obj));
                    break;
                default:
                    break;
            }
        }
    }
}
