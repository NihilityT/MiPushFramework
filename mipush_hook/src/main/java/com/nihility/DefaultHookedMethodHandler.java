package com.nihility;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;

import com.xiaomi.network.Fallback;
import com.xiaomi.push.service.MIPushNotificationHelper;
import com.xiaomi.push.service.XMPushService;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;

class DefaultHookedMethodHandler implements HookedMethodHandler {
    @Override
    public boolean shouldSendBroadcast(ProceedingJoinPoint joinPoint, XMPushService pushService, String packageName, XmPushActionContainer container, PushMetaInfo metaInfo) throws Throwable {
        return (boolean) joinPoint.proceed();
    }

    @Override
    public void postProcessMIPushMessage(ProceedingJoinPoint joinPoint, XMPushService pushService, String pkgName, byte[] payload, Intent newMessageIntent) throws Throwable {
        joinPoint.proceed();
    }

    @Override
    public void notifyPacketArrival(JoinPoint joinPoint, XMPushService pushService, String chid, Object data) {

    }

    @Override
    public Object debugLog(ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }

    @Override
    public void logFallback(JoinPoint joinPoint, Fallback fallback, boolean usePort) {

    }

    @Override
    public void processIntent(JoinPoint joinPoint, Intent intent) {

    }

    @Override
    public void onCreate(JoinPoint joinPoint, XMPushService pushService) throws Throwable {

    }

    @Override
    public void onStartCommand(JoinPoint joinPoint) {

    }

    @Override
    public void onStart(JoinPoint joinPoint, Intent intent, int startId) {

    }

    @Override
    public void onBind(JoinPoint joinPoint, Intent intent) {

    }

    @Override
    public void onDestroy(JoinPoint joinPoint) {

    }

    @Override
    public void setConnectionStatus(JoinPoint joinPoint, int newStatus, int reason, Exception e) {

    }

    @Override
    public void sendMessage(JoinPoint joinPoint, Intent intent) {

    }

    @Override
    public void logCheckServices(JoinPoint joinPoint, PackageInfo pkgInfo) {

    }

    @Override
    public Intent buildIntent(ProceedingJoinPoint joinPoint) throws Throwable {
        return (Intent) joinPoint.proceed();
    }

    @Override
    public XmPushActionContainer buildContainerHook(ProceedingJoinPoint joinPoint) throws Throwable {
        return (XmPushActionContainer) joinPoint.proceed();
    }

    @Override
    public boolean isIntentAvailable(ProceedingJoinPoint joinPoint) throws Throwable {
        return (boolean) joinPoint.proceed();
    }

    @Override
    public void processMIPushMessage(JoinPoint joinPoint, XMPushService pushService, byte[] decryptedContent, long packetBytesLen) {

    }

    @Override
    public boolean isDuplicateMessage(ProceedingJoinPoint joinPoint, XMPushService pushService, String packageName, String messageId) throws Throwable {
        return (boolean) joinPoint.proceed();
    }

    @Override
    public MIPushNotificationHelper.NotifyPushMessageInfo notifyPushMessage(ProceedingJoinPoint joinPoint, Context context, XmPushActionContainer container, byte[] decryptedContent) throws Throwable {
        return (MIPushNotificationHelper.NotifyPushMessageInfo) joinPoint.proceed();
    }
}
