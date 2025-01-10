package com.com.xiaomi.channel.commonutils.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.xiaomi.channel.commonutils.android.AppInfoUtils;
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


    @Around("execution(* com.xiaomi.channel.commonutils.android.AppInfoUtils.isAppRunning(..))" +
            "&& args(context, packageName)")
    public boolean isAppRunning(final ProceedingJoinPoint joinPoint,
                                Context context, String packageName) throws Throwable {
        return checkAwakeField(getLastMetaInfo())
                || isSystemApp(context, packageName)
                || (boolean) joinPoint.proceed();
    }

    public static boolean shouldSendBroadcast(Context context, String packageName, PushMetaInfo metaInfo) {
        setLastMetaInfo(metaInfo);
        return AppInfoUtils.isAppRunning(context, packageName);
    }

    private static boolean isSystemApp(Context context, String packageName) {
        try {
            return isSystemApp(getPackageFlags(context, packageName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static int getPackageFlags(Context context, String packageName) throws PackageManager.NameNotFoundException {
        return context.getPackageManager().getApplicationInfo(packageName, 0).flags;
    }

    private static boolean isSystemApp(int flags) {
        return (flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    private static boolean checkAwakeField(PushMetaInfo metaInfo) {
        boolean extraExists = metaInfo != null && metaInfo.extra != null;
        if (extraExists) {
            String awakeField = metaInfo.extra.get(PushConstants.EXTRA_PARAM_AWAKE);
            return Boolean.parseBoolean(awakeField);
        }
        return false;
    }
}
