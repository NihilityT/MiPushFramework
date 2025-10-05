package com.nihility;

import android.content.Intent;
import android.content.pm.PackageInfo;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.nihility.utils.Singleton;
import com.xiaomi.mipush.sdk.LogPushMessageProcessorAspect;
import com.xiaomi.mipush.sdk.ManifestCheckerAspectLog;
import com.xiaomi.network.Fallback;
import com.xiaomi.network.LogFallbackAspect;
import com.xiaomi.push.service.LogClientEventDispatcherAspect;
import com.xiaomi.push.service.LogDebugAspect;
import com.xiaomi.push.service.LogXMPushServiceAspect;
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

    public Object debugLog(final ProceedingJoinPoint joinPoint) throws Throwable {
        return Singleton.<LogDebugAspect>instance().logger(joinPoint);
    }

    public void logFallback(final JoinPoint joinPoint, Fallback fallback, boolean usePort) {
        Singleton.<LogFallbackAspect>instance().logFallback(joinPoint, fallback, usePort);
    }

    public void processIntent(final JoinPoint joinPoint, Intent intent) {
        Singleton.<LogPushMessageProcessorAspect>instance().processIntent(joinPoint, intent);
    }

    public void onCreate(final JoinPoint joinPoint, XMPushService pushService) throws Throwable {
        Singleton.<LogXMPushServiceAspect>instance().onCreate(joinPoint, pushService);
    }

    public void onStartCommand(final JoinPoint joinPoint) {
        Singleton.<LogXMPushServiceAspect>instance().onStartCommand(joinPoint);
    }

    public void onStart(final JoinPoint joinPoint, Intent intent, int startId) {
        Singleton.<LogXMPushServiceAspect>instance().onStart(joinPoint, intent, startId);
    }

    public void onBind(final JoinPoint joinPoint, Intent intent) {
        Singleton.<LogXMPushServiceAspect>instance().onBind(joinPoint, intent);
    }

    public void onDestroy(final JoinPoint joinPoint) {
        Singleton.<LogXMPushServiceAspect>instance().onDestroy(joinPoint);
    }

    public void setConnectionStatus(final JoinPoint joinPoint,
                                    int newStatus, int reason, Exception e) {
        Singleton.<LogXMPushServiceAspect>instance().setConnectionStatus(joinPoint, newStatus, reason, e);
    }

    public void sendMessage(final JoinPoint joinPoint, Intent intent) {
        Singleton.<LogXMPushServiceAspect>instance().sendMessage(joinPoint, intent);
    }

    public void logCheckServices(final JoinPoint joinPoint, PackageInfo pkgInfo) {
        Singleton.<ManifestCheckerAspectLog>instance().logCheckServices(joinPoint, pkgInfo);
    }

    public Intent buildIntent(final ProceedingJoinPoint joinPoint) throws Throwable {
        return Singleton.<MIPushEventProcessorAspect>instance().buildIntent(joinPoint);
    }

    public XmPushActionContainer buildContainerHook(final ProceedingJoinPoint joinPoint) throws Throwable {
        return Singleton.<MIPushEventProcessorAspect>instance().buildContainerHook(joinPoint);
    }

    public boolean isIntentAvailable(final ProceedingJoinPoint joinPoint) {
        return Singleton.<MIPushEventProcessorAspect>instance().isIntentAvailable(joinPoint);
    }

    public void processMIPushMessage(final JoinPoint joinPoint,
                                     XMPushService pushService, byte[] decryptedContent, long packetBytesLen) {
        Singleton.<MIPushEventProcessorAspect>instance().processMIPushMessage(joinPoint, pushService, decryptedContent, packetBytesLen);
    }
}