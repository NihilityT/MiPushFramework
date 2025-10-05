package com.nihility;

import android.content.ContextWrapper;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.com.xiaomi.channel.commonutils.android.AppInfoUtilsAspect;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.channel.commonutils.reflect.JavaCalls;
import com.xiaomi.push.service.MIPushEventProcessorAspect;
import com.xiaomi.push.service.PushConstants;
import com.xiaomi.push.service.XMPushService;
import com.xiaomi.xmpush.thrift.ActionType;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import org.aspectj.lang.ProceedingJoinPoint;

public class HookHandler {
    static final String TAG = HookHandler.class.getSimpleName();
    static final Logger logger = XLog.tag(TAG).build();
    public volatile boolean isPostProcessMIPushMessage = false;
    volatile boolean XMPushServiceHooked = false;

    public HookHandler() {
    }

    public boolean shouldSendBroadcast(
            final ProceedingJoinPoint joinPoint,
            XMPushService pushService, String packageName, XmPushActionContainer container, PushMetaInfo metaInfo) throws Throwable {
        joinPoint.proceed();
        if (container.action == ActionType.Registration) {
            return true;
        }
        if (container.packageName.startsWith("com.mi.")
                || container.packageName.startsWith("com.miui.")
                || container.packageName.startsWith("com.xiaomi.")) {
            return true;
        }
        XmPushActionContainer decorated = MIPushEventProcessorAspect.decoratedContainer(container.packageName, container);
        return AppInfoUtilsAspect.shouldSendBroadcast(pushService, packageName, decorated.metaInfo);
    }

    public void postProcessMIPushMessage(
            final ProceedingJoinPoint joinPoint,
            XMPushService pushService, String pkgName, byte[] payload, Intent newMessageIntent) throws Throwable {
        isPostProcessMIPushMessage = true;

        hookXMPushService(pushService);

        try {
            joinPoint.proceed();
        } finally {
            isPostProcessMIPushMessage = false;
        }
    }

    void hookXMPushService(XMPushService pushService) {
        if (XMPushServiceHooked || pushService == null) {
            return;
        }
        synchronized (this) {
            if (XMPushServiceHooked) {
                return;
            }
            try {
                ContextWrapper wrapped = new ContextWrapper(pushService.getBaseContext()) {
                    @Override
                    public void sendBroadcast(Intent intent, @Nullable String receiverPermission) {
                        if (isPostProcessMIPushMessage) {
                            byte[] payload = intent.getByteArrayExtra(PushConstants.MIPUSH_EXTRA_PAYLOAD);
                            Global.MiPushEventListener().transferToApplication(XMPushUtils.packToContainer(payload));
                        }
                        super.sendBroadcast(intent, receiverPermission);
                    }
                };
                JavaCalls.setField(pushService, "mBase", wrapped);
                XMPushServiceHooked = true;
            } catch (Throwable e) {
                logger.e("hook xmpushservice failed", e);
            }
        }
    }
}