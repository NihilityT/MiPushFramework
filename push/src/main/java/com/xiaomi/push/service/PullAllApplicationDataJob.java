package com.xiaomi.push.service;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.xiaomi.channel.commonutils.reflect.JavaCalls;
import com.xiaomi.mipush.sdk.PushContainerHelper;
import com.xiaomi.xmpush.thrift.ActionType;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmpush.thrift.XmPushActionNotification;
import com.xiaomi.xmpush.thrift.XmPushThriftSerializeUtils;

import java.util.Map;

import top.trumeet.common.utils.Utils;

class PullAllApplicationDataJob extends XMPushService.Job {

    private final XMPushService xmPushService;

    public PullAllApplicationDataJob(XMPushService xmPushService) {
        super(XMPushService.Job.TYPE_SEND_MSG);
        this.xmPushService = xmPushService;
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

            XmPushActionNotification notification2 = new XmPushActionNotification();
            notification2.setAppId(appId);
            notification2.setType("pull");
            notification2.setId("fake_pull_" + appId + "_" + System.currentTimeMillis());
            notification2.setRequireAck(false);

            XmPushActionContainer sendMsgContainer = JavaCalls.callStaticMethod(
                    PushContainerHelper.class.getName(), "generateRequestContainer",
                    Utils.getApplication(), notification2, ActionType.Notification,
                    false, packageName, appId);
            byte[] msgBytes = XmPushThriftSerializeUtils.convertThriftObjectToBytes(sendMsgContainer);
            xmPushService.sendMessage(packageName, msgBytes, false);
        }
    }
}
