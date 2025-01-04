package com.nihility.service;

import com.xiaomi.mipush.sdk.PushContainerHelper;
import com.xiaomi.push.service.XMPushService;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmpush.thrift.XmPushActionRegistrationResult;

import top.trumeet.common.utils.Utils;

public class RegistrationRecorder {
    public static void recordRegSec(XMPushService pushService, XmPushActionContainer container) {
        String regSec = getRegSec(pushService, container);
        Utils.setRegSec(container.getPackageName(), regSec);
    }

    public static String getRegSec(XMPushService pushService, XmPushActionContainer container) {
        try {
            XmPushActionRegistrationResult result = (XmPushActionRegistrationResult) PushContainerHelper.getResponseMessageBodyFromContainer(pushService, container);
            return result.getRegSecret();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}