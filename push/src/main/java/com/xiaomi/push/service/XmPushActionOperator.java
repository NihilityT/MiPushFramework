package com.xiaomi.push.service;

import androidx.annotation.NonNull;

import com.xiaomi.channel.commonutils.reflect.JavaCalls;
import com.xiaomi.mipush.sdk.PushContainerHelper;
import com.xiaomi.xmpush.thrift.ActionType;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmpush.thrift.XmPushActionNotification;
import com.xiaomi.xmpush.thrift.XmPushThriftSerializeUtils;

import org.apache.thrift.TBase;

import top.trumeet.common.utils.Utils;

public class XmPushActionOperator {
    private final XMPushService xmPushService;

    public XmPushActionOperator(XMPushService xmPushService) {
        this.xmPushService = xmPushService;
    }

    public void sendMessage(XmPushActionContainer sendMsgContainer, String packageName) {
        byte[] msgBytes = XmPushActionOperator.packToBytes(sendMsgContainer);
        xmPushService.sendMessage(packageName, msgBytes, false);
    }

    public static @NonNull XmPushActionContainer packToContainer(XmPushActionNotification action, String packageName) {
        return JavaCalls.callStaticMethod(
                PushContainerHelper.class.getName(), "generateRequestContainer",
                Utils.getApplication(), action, ActionType.Notification,
                false, packageName, action.appId);
    }

    public static <T extends TBase<T, ?>> byte[] packToBytes(T container) {
        return XmPushThriftSerializeUtils.convertThriftObjectToBytes(container);
    }
}