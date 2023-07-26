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

    @Before("execution(* com.xiaomi.mipush.sdk.PushMessageProcessor.processIntent(..))")
    public void processIntent(final JoinPoint joinPoint) {
        logger.d(joinPoint.getSignature());
        Intent intent = (Intent) joinPoint.getArgs()[0];
        logger.d("Intent " + ConvertUtils.toJson(intent));
    }

}
