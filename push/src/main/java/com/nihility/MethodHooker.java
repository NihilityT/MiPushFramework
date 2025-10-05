package com.nihility;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.xiaomi.network.Fallback;
import com.xiaomi.push.service.XMPushService;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
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

    @Around("(execution(* com.xiaomi.push.service.XMPushService*.*(..))" +
            " || execution(* com.xiaomi.push.service.PacketSync*.*(..))" +
            " || execution(* com.xiaomi.push.service.ClientEventDispatcher*.*(..))" +
            " || execution(* com.xiaomi.push.service.MIPushEventProcessor*.*(..))" +
            " || execution(* com.xiaomi.push.service.MIPushNotificationHelper*.*(..))" +
            " || execution(* com.xiaomi.push.service.NotificationManagerHelper*.*(..))" +
            ") && !within(is(FinalType))")
    public Object debugLog(final ProceedingJoinPoint joinPoint) throws Throwable {
        return hookHandler().debugLog(joinPoint);
    }

    @Before("execution(* com.xiaomi.network.Fallback.getHosts(..)) && target(fallback) && args(usePort)")
    public void logFallback(final JoinPoint joinPoint, Fallback fallback, boolean usePort) {
        hookHandler().logFallback(joinPoint, fallback, usePort);
    }

    @Before("execution(* com.xiaomi.mipush.sdk.PushMessageProcessor.processIntent(..)) && args(intent)")
    public void processIntent(final JoinPoint joinPoint, Intent intent) {
        hookHandler().processIntent(joinPoint, intent);
    }

    @After("execution(* com.xiaomi.push.service.XMPushService.onCreate(..)) && this(pushService)")
    public void onCreate(final JoinPoint joinPoint, XMPushService pushService) throws Throwable {
        hookHandler().onCreate(joinPoint, pushService);
    }


    @Before("execution(* com.xiaomi.push.service.XMPushService.onStartCommand(..))")
    public void onStartCommand(final JoinPoint joinPoint) {
        hookHandler().onStartCommand(joinPoint);
    }

    @Before("execution(* com.xiaomi.push.service.XMPushService.onStart(..)) && args(intent, startId)")
    public void onStart(final JoinPoint joinPoint, Intent intent, int startId) {
        hookHandler().onStart(joinPoint, intent, startId);
    }

    @Before("execution(* com.xiaomi.push.service.XMPushService.onBind(..)) && args(intent)")
    public void onBind(final JoinPoint joinPoint, Intent intent) {
        hookHandler().onBind(joinPoint, intent);
    }

    @Before("execution(* com.xiaomi.push.service.XMPushService.onDestroy(..))")
    public void onDestroy(final JoinPoint joinPoint) {
        hookHandler().onDestroy(joinPoint);
    }

    @Before("execution(* com.xiaomi.smack.Connection.setConnectionStatus(..)) && args(newStatus, reason, e)")
    public void setConnectionStatus(final JoinPoint joinPoint,
                                    int newStatus, int reason, Exception e) {
        hookHandler().setConnectionStatus(joinPoint, newStatus, reason, e);
    }

    @Before("execution(* com.xiaomi.push.service.XMPushService.sendMessage*(..)) && args(intent)")
    public void sendMessage(final JoinPoint joinPoint, Intent intent) {
        hookHandler().sendMessage(joinPoint, intent);
    }
}