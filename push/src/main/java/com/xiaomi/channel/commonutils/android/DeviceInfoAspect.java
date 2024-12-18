package com.xiaomi.channel.commonutils.android;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class DeviceInfoAspect {
    @Around("execution(* com.xiaomi.channel.commonutils.android.DeviceInfo.getMacAddress(..))")
    public Object avoidTracking(final JoinPoint joinPoint) {
        return "";
    }
}
