package com.xiaomi.mipush.sdk;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.text.TextUtils;

import com.xiaomi.push.service.PushConstants;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class ManifestCheckerAspect {
    @Around("execution(* com.xiaomi.mipush.sdk.ManifestChecker.checkServices(..)) && args(context, pkgInfo)")
    public Object ignoreWrongXmsfPermissionCheck(final ProceedingJoinPoint joinPoint, Context context, PackageInfo pkgInfo) throws Throwable {
        if (TextUtils.equals(pkgInfo.packageName, PushConstants.PUSH_SERVICE_PACKAGE_NAME)) {
            return null;
        }
        return joinPoint.proceed();
    }
}
