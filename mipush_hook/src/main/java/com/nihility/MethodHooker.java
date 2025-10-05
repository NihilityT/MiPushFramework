package com.nihility;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;

import androidx.annotation.NonNull;

import com.xiaomi.network.Fallback;
import com.xiaomi.push.service.MIPushNotificationHelper;
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
public class MethodHooker implements HookedMethodHandler {

    private static @NonNull HookedMethodHandler hookHandler() {
        HookedMethodHandler handler = Dependencies.instance().hookedMethodHandler();
        if (handler != null) {
            return handler;
        }
        return new DefaultHookedMethodHandler();
    }

    @Override
    @Around("execution(* com.xiaomi.push.service.MIPushEventProcessor.shouldSendBroadcast(..)) && args(pushService, packageName, container, metaInfo)")
    public boolean shouldSendBroadcast(
            final ProceedingJoinPoint joinPoint,
            XMPushService pushService, String packageName, XmPushActionContainer container, PushMetaInfo metaInfo) throws Throwable {
        return hookHandler().shouldSendBroadcast(joinPoint, pushService, packageName, container, metaInfo);
    }

    @Override
    @Around("execution(* com.xiaomi.push.service.MIPushEventProcessor.postProcessMIPushMessage(..)) && args(pushService, pkgName, payload, newMessageIntent)")
    public void postProcessMIPushMessage(
            final ProceedingJoinPoint joinPoint,
            XMPushService pushService, String pkgName, byte[] payload, Intent newMessageIntent) throws Throwable {
        hookHandler().postProcessMIPushMessage(joinPoint, pushService, pkgName, payload, newMessageIntent);
    }

    @Override
    @Before("execution(* com.xiaomi.push.service.ClientEventDispatcher.notifyPacketArrival(..)) && args(pushService, chid, data)")
    public void notifyPacketArrival(final JoinPoint joinPoint,
                                    XMPushService pushService, String chid, Object data) {
        hookHandler().notifyPacketArrival(joinPoint, pushService, chid, data);
    }

    @Override
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

    @Override
    @Before("execution(* com.xiaomi.network.Fallback.getHosts(..)) && target(fallback) && args(usePort)")
    public void logFallback(final JoinPoint joinPoint, Fallback fallback, boolean usePort) {
        hookHandler().logFallback(joinPoint, fallback, usePort);
    }

    @Override
    @Before("execution(* com.xiaomi.mipush.sdk.PushMessageProcessor.processIntent(..)) && args(intent)")
    public void processIntent(final JoinPoint joinPoint, Intent intent) {
        hookHandler().processIntent(joinPoint, intent);
    }

    @Override
    @After("execution(* com.xiaomi.push.service.XMPushService.onCreate(..)) && this(pushService)")
    public void onCreate(final JoinPoint joinPoint, XMPushService pushService) throws Throwable {
        hookHandler().onCreate(joinPoint, pushService);
    }


    @Override
    @Before("execution(* com.xiaomi.push.service.XMPushService.onStartCommand(..))")
    public void onStartCommand(final JoinPoint joinPoint) {
        hookHandler().onStartCommand(joinPoint);
    }

    @Override
    @Before("execution(* com.xiaomi.push.service.XMPushService.onStart(..)) && args(intent, startId)")
    public void onStart(final JoinPoint joinPoint, Intent intent, int startId) {
        hookHandler().onStart(joinPoint, intent, startId);
    }

    @Override
    @Before("execution(* com.xiaomi.push.service.XMPushService.onBind(..)) && args(intent)")
    public void onBind(final JoinPoint joinPoint, Intent intent) {
        hookHandler().onBind(joinPoint, intent);
    }

    @Override
    @Before("execution(* com.xiaomi.push.service.XMPushService.onDestroy(..))")
    public void onDestroy(final JoinPoint joinPoint) {
        hookHandler().onDestroy(joinPoint);
    }

    @Override
    @Before("execution(* com.xiaomi.smack.Connection.setConnectionStatus(..)) && args(newStatus, reason, e)")
    public void setConnectionStatus(final JoinPoint joinPoint,
                                    int newStatus, int reason, Exception e) {
        hookHandler().setConnectionStatus(joinPoint, newStatus, reason, e);
    }

    @Override
    @Before("execution(* com.xiaomi.push.service.XMPushService.sendMessage*(..)) && args(intent)")
    public void sendMessage(final JoinPoint joinPoint, Intent intent) {
        hookHandler().sendMessage(joinPoint, intent);
    }

    @Override
    @Before("execution(* com.xiaomi.mipush.sdk.ManifestChecker.checkServices(..)) && args(context, pkgInfo)")
    public void logCheckServices(final JoinPoint joinPoint, PackageInfo pkgInfo) {
        hookHandler().logCheckServices(joinPoint, pkgInfo);
    }

    @Override
    @Around("execution(* com.xiaomi.push.service.MIPushEventProcessor.buildIntent(..))")
    public Intent buildIntent(final ProceedingJoinPoint joinPoint) throws Throwable {
        return hookHandler().buildIntent(joinPoint);
    }

    @Override
    @Around("execution(* com.nihility.XMPushUtils.packToContainer(..))" +
            "|| execution(* com.xiaomi.push.service.MIPushEventProcessor.buildContainer(..))")
    public XmPushActionContainer buildContainerHook(final ProceedingJoinPoint joinPoint) throws Throwable {
        return hookHandler().buildContainerHook(joinPoint);
    }

    @Override
    @Around("execution(* com.xiaomi.push.service.MIPushEventProcessor.isIntentAvailable(..))")
    public boolean isIntentAvailable(final ProceedingJoinPoint joinPoint) throws Throwable {
        return hookHandler().isIntentAvailable(joinPoint);
    }

    @Override
    @Before("execution(* com.xiaomi.push.service.MIPushEventProcessor.processMIPushMessage(..)) && args(pushService, decryptedContent, packetBytesLen)")
    public void processMIPushMessage(final JoinPoint joinPoint,
                                     XMPushService pushService, byte[] decryptedContent, long packetBytesLen) {
        hookHandler().processMIPushMessage(joinPoint, pushService, decryptedContent, packetBytesLen);
    }

    @Override
    @Around("execution(* com.xiaomi.push.service.MiPushMessageDuplicate.isDuplicateMessage(..)) &&" + "args(pushService, packageName, messageId)")
    public boolean isDuplicateMessage(final ProceedingJoinPoint joinPoint, XMPushService pushService, String packageName, String messageId) throws Throwable {
        return hookHandler().isDuplicateMessage(joinPoint, pushService, packageName, messageId);
    }

    @Override
    @Around("execution(* com.xiaomi.push.service.MIPushNotificationHelper.notifyPushMessage(..)) &&" +
            "args(context, container, decryptedContent)")
    public MIPushNotificationHelper.NotifyPushMessageInfo notifyPushMessage(
            final ProceedingJoinPoint joinPoint, Context context, XmPushActionContainer container, byte[] decryptedContent) throws Throwable {
        return hookHandler().notifyPushMessage(joinPoint, context, container, decryptedContent);
    }

}