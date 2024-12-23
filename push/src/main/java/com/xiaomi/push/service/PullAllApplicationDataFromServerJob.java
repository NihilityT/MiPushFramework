package com.xiaomi.push.service;

import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.xiaomi.xmpush.thrift.XmPushActionNotification;

import java.util.Map;

import top.trumeet.common.utils.Utils;

public class PullAllApplicationDataFromServerJob extends XMPushService.Job {
    private final XmPushActionOperator xmPushActionOperator;

    public PullAllApplicationDataFromServerJob(XMPushService xmPushService) {
        super(XMPushService.Job.TYPE_SEND_MSG);
         xmPushActionOperator = new XmPushActionOperator(xmPushService);
    }

    @Override
    public String getDesc() {
        return "pull all application data";
    }

    @Override
    public void process() {
        SharedPreferences sp = Utils.getApplication().getSharedPreferences("pref_registered_pkg_names", 0);
        for (Map.Entry<String, ?> entry : sp.getAll().entrySet()) {
            String packageName = entry.getKey();
            String appId = entry.getValue() == null ? null : entry.getValue().toString();
            if (TextUtils.isEmpty(appId)) {
                continue;
            }

            xmPushActionOperator.sendMessage(
                    XmPushActionOperator.packToContainer(getPullAction(appId), packageName),
                    packageName);
        }
    }

    private static @NonNull XmPushActionNotification getPullAction(String appId) {
        XmPushActionNotification notification2 = new XmPushActionNotification();
        notification2.setAppId(appId);
        notification2.setType("pull");
        notification2.setId("fake_pull_" + appId + "_" + System.currentTimeMillis());
        notification2.setRequireAck(false);
        return notification2;
    }
}
