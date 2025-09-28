package com.nihility;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xiaomi.channel.commonutils.reflect.JavaCalls;
import com.xiaomi.mipush.sdk.PushContainerHelper;
import com.xiaomi.push.service.MIPushEventProcessor;
import com.xiaomi.xmpush.thrift.ActionType;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmpush.thrift.XmPushActionNotification;
import com.xiaomi.xmpush.thrift.XmPushThriftSerializeUtils;

import org.apache.thrift.TBase;

import top.trumeet.common.utils.CustomConfiguration;
import top.trumeet.common.utils.Utils;

public class XMPushUtils {
    public static @NonNull CustomConfiguration getConfiguration(XmPushActionContainer container) {
        if (container == null) {
            return new CustomConfiguration(null);
        }
        return getConfiguration(container.getMetaInfo());
    }

    public static @NonNull CustomConfiguration getConfiguration(PushMetaInfo metaInfo) {
        if (metaInfo == null) {
            return new CustomConfiguration(null);
        }
        return new CustomConfiguration(metaInfo.getExtra());
    }

    public static @Nullable XmPushActionContainer packToContainer(byte[] payload) {
        return MIPushEventProcessor.buildContainer(payload);
    }

    public static @NonNull XmPushActionContainer packToContainer(XmPushActionNotification action, String packageName) {
        return packToContainer(action, packageName, ActionType.Notification, action.appId);
    }

    public static @NonNull <T extends TBase<T, ?>> XmPushActionContainer packToContainer(T action, String packageName, ActionType actionType, String appId) {
        return JavaCalls.callStaticMethod(
                PushContainerHelper.class.getName(), "generateRequestContainer",
                Utils.getApplication(), action, actionType,
                false, packageName, appId);
    }

    public static <T extends TBase<T, ?>> byte[] packToBytes(T container) {
        return XmPushThriftSerializeUtils.convertThriftObjectToBytes(container);
    }
}