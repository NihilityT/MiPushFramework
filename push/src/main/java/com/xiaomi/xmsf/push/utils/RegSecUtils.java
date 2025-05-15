package com.xiaomi.xmsf.push.utils;

import android.text.TextUtils;

import com.xiaomi.push.service.XmPushActionOperator;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import top.trumeet.mipush.provider.entities.Event;

public class RegSecUtils {
    public static final String RegSecField = "__reg_sec__";

    public static String getRegSec(final XmPushActionContainer container) {
        if (container == null) {
            return null;
        }
        if (container.metaInfo != null && container.metaInfo.extra != null) {
            String regSec = container.metaInfo.extra.get(RegSecField);
            if (!TextUtils.isEmpty(regSec)) {
                return regSec;
            }
        }
        return top.trumeet.common.utils.Utils.getRegSec(container.packageName);
    }

    public static XmPushActionContainer getContainerWithRegSec(final Event event) {
        if (event == null || event.getPayload() == null) {
            return null;
        }
        XmPushActionContainer container = XmPushActionOperator.packToContainer(event.getPayload());
        if (container == null) {
            return null;
        }
        if (container.metaInfo == null || container.metaInfo.extra == null) {
            return container;
        }
        String regSec = event.getRegSec();
        if (!TextUtils.isEmpty(regSec)) {
            container.metaInfo.extra.put(RegSecField, regSec);
        }
        return container;
    }
}
