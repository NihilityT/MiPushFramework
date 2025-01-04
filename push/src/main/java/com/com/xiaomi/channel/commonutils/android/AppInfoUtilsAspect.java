package com.com.xiaomi.channel.commonutils.android;

import com.xiaomi.push.service.PushConstants;
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

    public static boolean checkAwakeField(PushMetaInfo metaInfo) {
        boolean extraExists = metaInfo != null && metaInfo.extra != null;
        if (extraExists) {
            String awakeField = metaInfo.extra.get(PushConstants.EXTRA_PARAM_AWAKE);
            return Boolean.parseBoolean(awakeField);
        }
        return false;
    }

    @Around("execution(* com.xiaomi.channel.commonutils.android.AppInfoUtils.isAppRunning(..))")
    public boolean isAppRunning(final ProceedingJoinPoint joinPoint) throws Throwable {
        return checkAwakeField(getLastMetaInfo());
    }
}
