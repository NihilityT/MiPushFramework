package com.com.xiaomi.channel.commonutils.android;

import static com.xiaomi.push.service.MIPushEventProcessorAspect.checkAwakeField;

import com.xiaomi.xmpush.thrift.PushMetaInfo;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class AppInfoUtilsAspect {
    static final private ThreadLocal<PushMetaInfo> metaInfo = new ThreadLocal<>();
    public static void setLastMetaInfo(PushMetaInfo metaInfo) {
        AppInfoUtilsAspect.metaInfo.set(metaInfo);
    }
    public static PushMetaInfo getLastMetaInfo() {
        return AppInfoUtilsAspect.metaInfo.get();
    }

    @Around("execution(* com.xiaomi.channel.commonutils.android.AppInfoUtils.isAppRunning(..))")
    public boolean isAppRunning(final ProceedingJoinPoint joinPoint) throws Throwable {
        return checkAwakeField(getLastMetaInfo());
    }
}
