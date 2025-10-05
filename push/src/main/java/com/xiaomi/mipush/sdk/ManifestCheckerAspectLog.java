package com.xiaomi.mipush.sdk;

import android.content.pm.PackageInfo;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import org.aspectj.lang.JoinPoint;

public class ManifestCheckerAspectLog {
    private static final String TAG = ManifestCheckerAspect.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    public void logCheckServices(final JoinPoint joinPoint, PackageInfo pkgInfo) {
        logger.d(joinPoint.getSignature());
        logger.d(pkgInfo);
    }
}
