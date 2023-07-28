package com.xiaomi.mipush.sdk;

import android.content.Intent;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.xmsf.utils.ConvertUtils;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class PushMessageProcessorAspect {
    private static final String TAG = PushMessageProcessorAspect.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    @Before("execution(* com.xiaomi.mipush.sdk.PushMessageProcessor.processIntent(..)) && args(intent)")
    public void processIntent(final JoinPoint joinPoint, Intent intent) {
        logger.d(joinPoint.getSignature());
        logger.d("Intent " + ConvertUtils.toJson(intent));
    }

}
