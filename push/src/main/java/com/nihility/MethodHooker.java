package com.nihility;

import android.content.Intent;

import com.com.xiaomi.channel.commonutils.android.AppInfoUtilsAspect;
import com.xiaomi.push.service.MIPushEventProcessorAspect;
import com.xiaomi.push.service.XMPushService;
import com.xiaomi.xmpush.thrift.ActionType;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import org.aspectj.lang.ProceedingJoinPoint;

public class MethodHooker {

    private static class LazyHolder {
        static MethodHooker INSTANCE = new MethodHooker();
    }

    private static MethodHooker instance;

    public static MethodHooker instance() {
        return instance != null ? instance : LazyHolder.INSTANCE;
    }

    public static void setInstance(MethodHooker methodHooker) {
        instance = methodHooker;
    }

    public boolean shouldSendBroadcast(
            final ProceedingJoinPoint joinPoint,
            XMPushService pushService, String packageName, XmPushActionContainer container, PushMetaInfo metaInfo) throws Throwable {
        joinPoint.proceed();
        if (container.action == ActionType.Registration) {
            return true;
        }
        if (container.packageName.startsWith("com.mi.")
                || container.packageName.startsWith("com.miui.")
                || container.packageName.startsWith("com.xiaomi.")) {
            return true;
        }
        XmPushActionContainer decorated = MIPushEventProcessorAspect.decoratedContainer(container.packageName, container);
        return AppInfoUtilsAspect.shouldSendBroadcast(pushService, packageName, decorated.metaInfo);
    }
}