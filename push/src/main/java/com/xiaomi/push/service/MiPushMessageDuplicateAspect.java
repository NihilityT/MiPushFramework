package com.xiaomi.push.service;

import android.text.TextUtils;

import com.xiaomi.channel.commonutils.reflect.JavaCalls;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmsf.utils.ConvertUtils;

import org.apache.thrift.TBase;
import org.aspectj.lang.ProceedingJoinPoint;

public class MiPushMessageDuplicateAspect {
    public static String mockId;

    public static void markAsMock(XmPushActionContainer container) {
        mockId = getMessageId(container);
    }

    static String getMessageId(XmPushActionContainer container) {
        PushMetaInfo metaInfo = container.metaInfo;
        if (metaInfo == null) {
            return getMessageIdFromPushAction(container);
        }
        if (metaInfo.extra != null) {
            String jobId = metaInfo.extra.get(PushConstants.EXTRA_JOB_KEY);
            if (!TextUtils.isEmpty(jobId)) {
                return jobId;
            }
        }
        return metaInfo.getId();
    }

    private static String getMessageIdFromPushAction(XmPushActionContainer container) {
        try {
            TBase pushAction = ConvertUtils.getResponseMessageBodyFromContainer(container, null);
            return JavaCalls.getField(pushAction, "id");
        } catch (Throwable ignored) {
            return null;
        }
    }

    static boolean isMockMessage(XmPushActionContainer container) {
        return TextUtils.equals(getMessageId(container), mockId);
    }

    public boolean isDuplicateMessage(final ProceedingJoinPoint joinPoint, XMPushService pushService, String packageName, String messageId) throws Throwable {
        if (messageId.equals(mockId)) {
            mockId = null;
            return false;
        }
        return (boolean) joinPoint.proceed();
    }
}
