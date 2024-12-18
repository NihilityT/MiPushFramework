package com.xiaomi.channel.commonutils.android;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class MIUIUtilsAspect {
    @Around("(execution(* com.xiaomi.channel.commonutils.android.MIUIUtils.getProperty(..))" +
            " || execution(* com.xiaomi.channel.commonutils.android.SystemProperties.get(..)))" +
            "&& args(key, ..)")
    public Object hookAllCountryCodeToCN(ProceedingJoinPoint joinPoint, String key) throws Throwable {
        if (key != null && (key.contains(".region") || key.contains(".country"))) {
            return "CN";
        }
        return joinPoint.proceed();
    }
}
