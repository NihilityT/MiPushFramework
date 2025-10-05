package com.xiaomi.mipush.sdk;

import android.content.Intent;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.xmsf.utils.ConvertUtils;

import org.aspectj.lang.JoinPoint;

public class LogPushMessageProcessorAspect {
    private static final String TAG = LogPushMessageProcessorAspect.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    public void processIntent(final JoinPoint joinPoint, Intent intent) {
        logger.d(joinPoint.getSignature());
        logger.d("Intent " + ConvertUtils.toJson(intent));
    }

}
