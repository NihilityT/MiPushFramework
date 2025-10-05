package com.nihility;

import android.content.Intent;

import com.xiaomi.push.service.XMPushService;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import org.aspectj.lang.ProceedingJoinPoint;

public class MethodHooker {
    public final HookHandler hookHandler = new HookHandler();

    public boolean shouldSendBroadcast(
            final ProceedingJoinPoint joinPoint,
            XMPushService pushService, String packageName, XmPushActionContainer container, PushMetaInfo metaInfo) throws Throwable {
        return hookHandler.shouldSendBroadcast(joinPoint, pushService, packageName, container, metaInfo);
    }

    public void postProcessMIPushMessage(
            final ProceedingJoinPoint joinPoint,
            XMPushService pushService, String pkgName, byte[] payload, Intent newMessageIntent) throws Throwable {

        hookHandler.postProcessMIPushMessage(joinPoint, pushService, pkgName, payload, newMessageIntent);
    }

}