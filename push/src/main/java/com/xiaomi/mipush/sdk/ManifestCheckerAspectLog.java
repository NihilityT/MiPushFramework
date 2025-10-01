package com.xiaomi.mipush.sdk;

import android.content.pm.PackageInfo;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class ManifestCheckerAspectLog {
    private static final String TAG = ManifestCheckerAspect.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    @Before("execution(* com.xiaomi.mipush.sdk.ManifestChecker.checkServices(..)) && args(context, pkgInfo)")
    public void logCheckServices(final JoinPoint joinPoint, PackageInfo pkgInfo) {
        logger.d(joinPoint.getSignature());
        logger.d(pkgInfo);
    }
}
