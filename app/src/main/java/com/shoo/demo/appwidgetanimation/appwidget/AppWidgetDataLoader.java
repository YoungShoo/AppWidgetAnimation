package com.shoo.demo.appwidgetanimation.appwidget;

import android.support.annotation.NonNull;

import com.shoo.demo.appwidgetanimation.data.BlockItem;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;

/**
 * 桌面插件数据加载器
 *
 * Created by Shoo on 16-12-25.
 */
public class AppWidgetDataLoader {

    private static AppWidgetDataLoader sInstance;

    private List<BlockItem> mLastData;

    private static class Holder {
        public static final AppWidgetDataLoader INSTANCE = new AppWidgetDataLoader();
    }

    public static AppWidgetDataLoader getInstance() {
        if (sInstance == null) {
            sInstance = Holder.INSTANCE;
        }
        return sInstance;
    }

    private AppWidgetDataLoader() {

    }

    protected Observable<List<BlockItem>> start() {
        return Observable.just(createBlockItems());
    }

    @NonNull
    private List<BlockItem> createBlockItems() {
        List<BlockItem> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(new BlockItem("item " + (i + 1)));
        }
        return list;
    }

    public void saveData(List<BlockItem> blockItems) {
        mLastData = blockItems;
    }

    private List<BlockItem> getData() {
        return mLastData;
    }

    public Observable<List<BlockItem>> getDataObservable() {
        return Observable
                .just(getData())
                .flatMap(new Func1<List<BlockItem>, Observable<List<BlockItem>>>() {
                    @Override
                    public Observable<List<BlockItem>> call(List<BlockItem> blockItems) {
                        if (blockItems == null || blockItems.isEmpty()) {
                            return Observable.just(createBlockItems());
                        }
                        return Observable.just(blockItems);
                    }
                });
    }
}