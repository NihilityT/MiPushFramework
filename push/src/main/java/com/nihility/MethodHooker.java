package com.nihility;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.xiaomi.push.service.XMPushService;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class MethodHooker {

    private static @NonNull HookHandler hookHandler() {
        return Global.HookHandler();
    }

    @Around("execution(* com.xiaomi.push.service.MIPushEventProcessor.shouldSendBroadcast(..)) && args(pushService, packageName, container, metaInfo)")
    public boolean shouldSendBroadcast(
            final ProceedingJoinPoint joinPoint,
            XMPushService pushService, String packageName, XmPushActionContainer container, PushMetaInfo metaInfo) throws Throwable {
        return hookHandler().shouldSendBroadcast(joinPoint, pushService, packageName, container, metaInfo);
    }

    @Around("execution(* com.xiaomi.push.service.MIPushEventProcessor.postProcessMIPushMessage(..)) && args(pushService, pkgName, payload, newMessageIntent)")
    public void postProcessMIPushMessage(
            final ProceedingJoinPoint joinPoint,
            XMPushService pushService, String pkgName, byte[] payload, Intent newMessageIntent) throws Throwable {
        hookHandler().postProcessMIPushMessage(joinPoint, pushService, pkgName, payload, newMessageIntent);
    }

    @Before("execution(* com.xiaomi.push.service.ClientEventDispatcher.notifyPacketArrival(..)) && args(pushService, chid, data)")
    public void notifyPacketArrival(final JoinPoint joinPoint,
                                    XMPushService pushService, String chid, Object data) {
        hookHandler().notifyPacketArrival(joinPoint, pushService, chid, data);
    }
}