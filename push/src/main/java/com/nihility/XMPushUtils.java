package com.nihility;

import androidx.annotation.NonNull;

import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import top.trumeet.common.utils.CustomConfiguration;

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
}