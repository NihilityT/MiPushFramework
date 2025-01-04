package com.xiaomi.push.service;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class MiPushMessageDuplicateAspect {
    public static String mockId;

    @Around("execution(* com.xiaomi.push.service.MiPushMessageDuplicate.isDuplicateMessage(..)) &&" + "args(pushService, packageName, messageId)")
    public boolean isDuplicateMessage(final ProceedingJoinPoint joinPoint, XMPushService pushService, String packageName, String messageId) throws Throwable {
        if (messageId.equals(mockId)) {
            mockId = null;
            return false;
        }
        return (boolean) joinPoint.proceed();
    }
}
