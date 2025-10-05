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

public interface HookedMethodHandler {
    boolean shouldSendBroadcast(
            ProceedingJoinPoint joinPoint,
            XMPushService pushService, String packageName, XmPushActionContainer container, PushMetaInfo metaInfo) throws Throwable;

    void postProcessMIPushMessage(
            ProceedingJoinPoint joinPoint,
            XMPushService pushService, String pkgName, byte[] payload, Intent newMessageIntent) throws Throwable;

    void notifyPacketArrival(JoinPoint joinPoint,
                             XMPushService pushService, String chid, Object data);

    Object debugLog(ProceedingJoinPoint joinPoint) throws Throwable;

    void logFallback(JoinPoint joinPoint, Fallback fallback, boolean usePort);

    void processIntent(JoinPoint joinPoint, Intent intent);

    void onCreate(JoinPoint joinPoint, XMPushService pushService) throws Throwable;

    void onStartCommand(JoinPoint joinPoint);

    void onStart(JoinPoint joinPoint, Intent intent, int startId);

    void onBind(JoinPoint joinPoint, Intent intent);

    void onDestroy(JoinPoint joinPoint);

    void setConnectionStatus(JoinPoint joinPoint,
                             int newStatus, int reason, Exception e);

    void sendMessage(JoinPoint joinPoint, Intent intent);

    void logCheckServices(JoinPoint joinPoint, PackageInfo pkgInfo);

    Intent buildIntent(ProceedingJoinPoint joinPoint) throws Throwable;

    XmPushActionContainer buildContainerHook(ProceedingJoinPoint joinPoint) throws Throwable;

    boolean isIntentAvailable(ProceedingJoinPoint joinPoint);

    void processMIPushMessage(JoinPoint joinPoint,
                              XMPushService pushService, byte[] decryptedContent, long packetBytesLen);

    boolean isDuplicateMessage(ProceedingJoinPoint joinPoint, XMPushService pushService, String packageName, String messageId) throws Throwable;

    MIPushNotificationHelper.NotifyPushMessageInfo notifyPushMessage(
            ProceedingJoinPoint joinPoint, Context context, XmPushActionContainer container, byte[] decryptedContent);
}
