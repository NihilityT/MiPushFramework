package com.nihility.service;

import android.content.Context;

import com.xiaomi.mipush.sdk.PushContainerHelper;
import com.xiaomi.xmpush.thrift.ActionType;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmpush.thrift.XmPushActionRegistrationResult;

import top.trumeet.common.utils.Utils;

public class RegistrationRecorder {
    Context context;

    private static class LazyHolder {
        static RegistrationRecorder INSTANCE = new RegistrationRecorder();
    }

    public static RegistrationRecorder getInstance() {
        return LazyHolder.INSTANCE;
    }

    public static void setInstance(RegistrationRecorder instance) {
        LazyHolder.INSTANCE = instance;
    }

    public void initContext(Context context) {
        this.context = context;
    }

    public void recordRegSec(XmPushActionContainer container) {
        if (ActionType.Registration != container.getAction()) {
            return;
        }
        String regSec = getRegSec(context, container);
        Utils.setRegSec(container.getPackageName(), regSec);
    }

    public static String getRegSec(Context pushService, XmPushActionContainer container) {
        try {
            XmPushActionRegistrationResult result = (XmPushActionRegistrationResult) PushContainerHelper.getResponseMessageBodyFromContainer(pushService, container);
            return result.getRegSecret();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}