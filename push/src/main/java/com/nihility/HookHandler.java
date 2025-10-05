package com.nihility;

import android.content.Intent;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.nihility.utils.Singleton;
import com.xiaomi.push.service.LogClientEventDispatcherAspect;
import com.xiaomi.push.service.MIPushEventProcessorAspect;
import com.xiaomi.push.service.XMPushService;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;

public class HookHandler {
    static final String TAG = HookHandler.class.getSimpleName();
    static final Logger logger = XLog.tag(TAG).build();

    public HookHandler() {
    }

    public boolean shouldSendBroadcast(
            final ProceedingJoinPoint joinPoint,
            XMPushService pushService, String packageName, XmPushActionContainer container, PushMetaInfo metaInfo) throws Throwable {
        return Singleton.<MIPushEventProcessorAspect>instance().shouldSendBroadcast(joinPoint, pushService, packageName, container, metaInfo);
    }

    public void postProcessMIPushMessage(
            final ProceedingJoinPoint joinPoint,
            XMPushService pushService, String pkgName, byte[] payload, Intent newMessageIntent) throws Throwable {
        Singleton.<MIPushEventProcessorAspect>instance().postProcessMIPushMessage(joinPoint, pushService, pkgName, payload, newMessageIntent);
    }

    public void notifyPacketArrival(final JoinPoint joinPoint,
                                    XMPushService pushService, String chid, Object data) {
        Singleton.<LogClientEventDispatcherAspect>instance().notifyPacketArrival(joinPoint, pushService, chid, data);
    }
}