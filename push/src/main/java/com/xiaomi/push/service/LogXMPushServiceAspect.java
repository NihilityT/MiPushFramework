package com.xiaomi.push.service;

import android.content.Intent;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.nihility.Global;
import com.xiaomi.xmsf.utils.ConvertUtils;

import org.aspectj.lang.JoinPoint;

public class LogXMPushServiceAspect {
    private static final String TAG = LogXMPushServiceAspect.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    public void onCreate(final JoinPoint joinPoint, XMPushService pushService) throws Throwable {
        logger.d(joinPoint.getSignature());
        logger.d("Service started");
    }

    public void onStartCommand(final JoinPoint joinPoint) {
        logger.d(joinPoint.getSignature());
    }

    public void onStart(final JoinPoint joinPoint, Intent intent, int startId) {
        logger.d(joinPoint.getSignature());
        logIntent(intent);
        Global.MiPushEventListener().receiveFromApplication(intent);
    }

    public void onBind(final JoinPoint joinPoint, Intent intent) {
        logger.d(joinPoint.getSignature());
        logIntent(intent);
    }

    public void onDestroy(final JoinPoint joinPoint) {
        logger.d(joinPoint.getSignature());
        logger.d("Service stopped");
    }

    public void setConnectionStatus(final JoinPoint joinPoint,
                                    int newStatus, int reason, Exception e) {
        logger.d(joinPoint.getSignature());
    }

    public void sendMessage(final JoinPoint joinPoint, Intent intent) {
        logger.d(joinPoint.getSignature());
        Global.MiPushEventListener().transferToServer(intent);
    }

    private void logIntent(Intent intent) {
        logger.d("Intent" + " " + ConvertUtils.toJson(intent));
    }
}
