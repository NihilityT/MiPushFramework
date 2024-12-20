package com.xiaomi.mipush.sdk;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.text.TextUtils;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.push.service.PushConstants;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class ManifestCheckerAspect {
    private static final String TAG = ManifestCheckerAspect.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    @Around("execution(* com.xiaomi.mipush.sdk.ManifestChecker.checkServices(..)) && args(context, pkgInfo)")
    public Object ignoreWrongXmsfPermissionCheck(final ProceedingJoinPoint joinPoint, Context context, PackageInfo pkgInfo) throws Throwable {
        if (TextUtils.equals(pkgInfo.packageName, PushConstants.PUSH_SERVICE_PACKAGE_NAME)) {
            return null;
        }
        return joinPoint.proceed();
    }

    @Before("execution(* com.xiaomi.mipush.sdk.ManifestChecker.checkServices(..)) && args(context, pkgInfo)")
    public void logCheckServices(final JoinPoint joinPoint, PackageInfo pkgInfo) {
        logger.d(joinPoint.getSignature());
        logger.d(pkgInfo);
    }
}
