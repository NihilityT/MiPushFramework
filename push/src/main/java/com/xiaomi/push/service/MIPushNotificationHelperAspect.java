package com.xiaomi.push.service;

import android.content.Context;

import com.nihility.Global;
import com.nihility.Hooked;
import com.xiaomi.push.service.MIPushNotificationHelper.NotifyPushMessageInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class MIPushNotificationHelperAspect {

    @Around("execution(* com.xiaomi.push.service.MIPushNotificationHelper.notifyPushMessage(..)) &&" +
            "args(context, container, decryptedContent)")
    public NotifyPushMessageInfo notifyPushMessage(
            final ProceedingJoinPoint joinPoint, Context context, XmPushActionContainer container, byte[] decryptedContent) {
        Hooked.mark("MIPushNotificationHelper.NotifyPushMessageInfo");
        Global.MiPushEventListener().transferToApplication(container);
        MyMIPushNotificationHelper.notifyPushMessage(context, decryptedContent);
        return new NotifyPushMessageInfo();
    }
}
