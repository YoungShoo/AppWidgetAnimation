package com.shoo.demo.appwidgetanimation.appwidget;

import android.widget.RemoteViews;

import com.shoo.demo.appwidgetanimation.MyApplication;
import com.shoo.demo.appwidgetanimation.R;
import com.shoo.demo.appwidgetanimation.data.BlockItem;
import com.shoo.demo.appwidgetanimation.util.ResourceUtils;

import java.util.List;

/**
 * 用于创建桌面插件控件
 *
 * Created by Shoo on 16-11-12.
 */

public class RemoteViewsBuilder {

    /**
     * 创建列表
     *
     * @param list
     * @param start
     * @param end
     * @param anim
     * @return
     */
    public static RemoteViews buildListView(List<BlockItem> list, int start, int end, EAnimation anim) {
        if (list == null || list.size() < end || start < 0 || start >= end) {
            return null;
        }

        RemoteViews remoteViews;
        if (EAnimation.SLIDE_IN_LEFT == anim) {
            remoteViews = new RemoteViews(ResourceUtils.getPackageName(), R.layout.wg_anim_layout_in_left);
        } else if (EAnimation.SLIDE_OUT_RIGHT == anim) {
            remoteViews = new RemoteViews(ResourceUtils.getPackageName(), R.layout.wg_anim_layout_out_right);
        } else if (EAnimation.SLIDE_IN_RIGHT == anim) {
            remoteViews = new RemoteViews(ResourceUtils.getPackageName(), R.layout.wg_anim_layout_in_right);
        } else if (EAnimation.SLIDE_OUT_LEFT == anim) {
            remoteViews = new RemoteViews(ResourceUtils.getPackageName(), R.layout.wg_anim_layout_out_left);
        } else {
            remoteViews = new RemoteViews(ResourceUtils.getPackageName(), R.layout.wg_anim_layout_none);
        }

        for (int i = start; i < end; i++) {
            // 添加列表项
            RemoteViews itemView = buildItemView(list, i);
            if (itemView != null) {
                remoteViews.addView(R.id.wg_list_view, itemView);
            }
        }

        return remoteViews;
    }

    /**
     * 创建单个列表项
     *
     * @param list
     * @param pos
     * @return
     */
    private static RemoteViews buildItemView(List<BlockItem> list, int pos) {
        if (list == null || list.size() <= pos || pos < 0) {
            return null;
        }

        RemoteViews remoteViews = new RemoteViews(ResourceUtils.getPackageName(), R.layout.wg_list_item_view);
        remoteViews.setTextViewText(R.id.title, list.get(pos).getTitle());

        remoteViews.setOnClickPendingIntent(R.id.wg_list_item_container, AppWidgetManagerService
                .getItemClickPendingIntent(MyApplication.getAppContext(), pos));

        return remoteViews;
    }

    /**
     * 获取桌面插件视图
     *
     * @return
     */
    public static RemoteViews buildRootRemoteViews() {
        return new RemoteViews(ResourceUtils.getPackageName(), R.layout.wg_initial_layout);
    }
}
