package com.shoo.demo.appwidgetanimation.util;

import android.os.Looper;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by Shoo on 15-12-30.
 */
public class RxUtils {

    private static Scheduler.Worker sMainThreadWorker;

    /**
     * 取消订阅
     *
     * @param subscription
     */
    public static void unsubscribe(Subscription subscription) {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }

    /**
     * 主线程执行任务
     *
     * @param action
     */
    public static Subscription scheduleOnMainThread(final Action0 action) {
        // 当前是主线程，直接执行
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.call();
            return null;
        }
        return getMainThreadWorker().schedule(action);
    }

    /**
     * 主线程执行任务
     *
     * @param action
     * @param delayTime
     * @param unit
     */
    public static Subscription scheduleOnMainThread(final Action0 action, final long delayTime, final
    TimeUnit unit) {
        return getMainThreadWorker().schedule(action, delayTime, unit);
    }

    /**
     * 获取主线程任务处理器
     *
     * @return
     */
    public static synchronized Scheduler.Worker getMainThreadWorker() {
        if (sMainThreadWorker == null) {
            sMainThreadWorker = AndroidSchedulers.mainThread().createWorker();
        }
        return sMainThreadWorker;
    }

    /**
     * 异步线程执行任务
     *
     * @param action
     * @return
     */
    public static Subscription scheduleOnIoThread(final Action0 action) {
        // 当前是异步线程，直接执行
        if (Looper.myLooper() != Looper.getMainLooper()) {
            action.call();
            return null;
        }
        return createIoThreadWorker().schedule(action);
    }

    /**
     * 异步线程执行任务
     *
     * @param action
     * @param delayTime
     * @param unit
     * @return
     */
    public static Subscription scheduleOnIoThread(final Action0 action, final long delayTime, final
    TimeUnit unit) {
        return createIoThreadWorker().schedule(action, delayTime, unit);
    }

    /**
     * 创建IO异步任务处理器
     *
     * @return
     */
    public static Scheduler.Worker createIoThreadWorker() {
        return Schedulers.io().createWorker();
    }

    /**
     * 在当前线程执行任务
     *
     * @param action
     * @param delay
     * @param timeUnit
     */
    public static Subscription scheduleOnCurThread(Action0 action, long delay, TimeUnit timeUnit) {
        return Schedulers.immediate().createWorker().schedule(action, delay, timeUnit);
    }

    public static <T> Observable.Transformer<T, T> applyAsyncScheduler() {
        return new Observable.Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> observable) {
                return observable
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread());
            }
        };
    }

    public static <T> Observable.Transformer<List<T>, List<T>> takeListCountTransformer(final int count) {
        return new Observable.Transformer<List<T>, List<T>>() {
            @Override
            public Observable<List<T>> call(Observable<List<T>> listObservable) {
                return listObservable
                        .map(new Func1<List<T>, List<T>>() {
                            @Override
                            public List<T> call(List<T> originList) {
                                if (originList != null && originList.size() > count) {
                                    return originList.subList(0, count);
                                }
                                return originList;
                            }
                        });
            }
        };
    }

    public static <T> Func1<List<T>, List<T>> takeListCount(final int count) {
        return new Func1<List<T>, List<T>>() {
            @Override
            public List<T> call(List<T> list) {
                if (list != null && list.size() > count) {
                    return list.subList(0, count);
                }
                return list;
            }
        };
    }

    public static <T> Observable.Transformer<T, T> callOnNextAndOnError(final Action0 action) {
        return new Observable.Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> listObservable) {
                return listObservable
                        .doOnNext(new Action1<T>() {
                            @Override
                            public void call(T t) {
                                try {
                                    action.call();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                        .doOnError(new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                try {
                                    action.call();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
            }
        };
    }
}
