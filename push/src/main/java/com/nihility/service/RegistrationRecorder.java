package com.nihility.service;

import android.content.Context;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.nihility.utils.Singleton;
import com.xiaomi.mipush.sdk.PushContainerHelper;
import com.xiaomi.xmpush.thrift.ActionType;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmpush.thrift.XmPushActionRegistrationResult;

import top.trumeet.common.utils.Utils;

public class RegistrationRecorder {
    private static final String TAG = RegistrationRecorder.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    Context context;

    public static RegistrationRecorder getInstance() {
        return Singleton.instance();
    }

    public static void setInstance(RegistrationRecorder instance) {
        Singleton.reset(instance);
    }

    public void initContext(Context context) {
        this.context = context;
    }

    public void recordRegSec(XmPushActionContainer container) {
        if (container == null || container.isRequest || container.action != ActionType.Registration) {
            return;
        }
        String regSec = getRegSec(context, container);
        if (regSec != null) {
            Utils.setRegSec(container.getPackageName(), regSec);
        }
    }

    public static String getRegSec(Context pushService, XmPushActionContainer container) {
        try {
            XmPushActionRegistrationResult result = (XmPushActionRegistrationResult) PushContainerHelper.getResponseMessageBodyFromContainer(pushService, container);
            return result.getRegSecret();
        } catch (Throwable e) {
            logger.e("cannot save RegSec", e);
        }
        return null;
    }
}