package com.xiaomi.push.service;

import android.content.Intent;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.xmsf.utils.ConvertUtils;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class LogXMPushServiceAspect {
    private static final String TAG = LogXMPushServiceAspect.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    @After("execution(* com.xiaomi.push.service.XMPushService.onCreate(..)) && this(pushService)")
    public void onCreate(final JoinPoint joinPoint, XMPushService pushService) throws Throwable {
        logger.d(joinPoint.getSignature());
        logger.d("Service started");
    }


    @Before("execution(* com.xiaomi.push.service.XMPushService.onStartCommand(..))")
    public void onStartCommand(final JoinPoint joinPoint) {
        logger.d(joinPoint.getSignature());
    }

    @Before("execution(* com.xiaomi.push.service.XMPushService.onStart(..)) && args(intent, startId)")
    public void onStart(final JoinPoint joinPoint, Intent intent, int startId) {
        logger.d(joinPoint.getSignature());
        logIntent(intent);
    }

    @Before("execution(* com.xiaomi.push.service.XMPushService.onBind(..)) && args(intent)")
    public void onBind(final JoinPoint joinPoint, Intent intent) {
        logger.d(joinPoint.getSignature());
        logIntent(intent);
    }

    @Before("execution(* com.xiaomi.push.service.XMPushService.onDestroy(..))")
    public void onDestroy(final JoinPoint joinPoint) {
        logger.d(joinPoint.getSignature());
        logger.d("Service stopped");
    }

    @Before("execution(* com.xiaomi.smack.Connection.setConnectionStatus(..)) && args(newStatus, reason, e)")
    public void setConnectionStatus(final JoinPoint joinPoint,
                                    int newStatus, int reason, Exception e) {
        logger.d(joinPoint.getSignature());
    }

    private void logIntent(Intent intent) {
        logger.d("Intent" + " " + ConvertUtils.toJson(intent));
    }

}
