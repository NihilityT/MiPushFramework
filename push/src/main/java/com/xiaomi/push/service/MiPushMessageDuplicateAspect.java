package com.xiaomi.push.service;

import android.text.TextUtils;

import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class MiPushMessageDuplicateAspect {
    public static String mockId;

    public static void markAsMock(XmPushActionContainer container) {
        mockId = getMessageId(container);
    }

    static String getMessageId(XmPushActionContainer container) {
        PushMetaInfo metaInfo = container.metaInfo;
        if (metaInfo == null) {
            return null;
        }
        if (metaInfo.extra != null) {
            String jobId = metaInfo.extra.get(PushConstants.EXTRA_JOB_KEY);
            if (!TextUtils.isEmpty(jobId)) {
                return jobId;
            }
        }
        return metaInfo.getId();
    }

    static boolean isMockMessage(XmPushActionContainer container) {
        return TextUtils.equals(getMessageId(container), mockId);
    }

    @Around("execution(* com.xiaomi.push.service.MiPushMessageDuplicate.isDuplicateMessage(..)) &&" + "args(pushService, packageName, messageId)")
    public boolean isDuplicateMessage(final ProceedingJoinPoint joinPoint, XMPushService pushService, String packageName, String messageId) throws Throwable {
        if (messageId.equals(mockId)) {
            mockId = null;
            return false;
        }
        return (boolean) joinPoint.proceed();
    }
}
