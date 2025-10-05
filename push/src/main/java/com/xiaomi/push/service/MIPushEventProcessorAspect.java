package com.xiaomi.push.service;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.com.xiaomi.channel.commonutils.android.AppInfoUtilsAspect;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.nihility.Global;
import com.nihility.XMPushUtils;
import com.xiaomi.push.service.clientReport.ReportConstants;
import com.xiaomi.xmpush.thrift.ActionType;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmsf.push.utils.Configurations;
import com.xiaomi.xmsf.utils.ConvertUtils;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import top.trumeet.mipush.provider.db.EventDb;
import top.trumeet.mipush.provider.db.RegisteredApplicationDb;
import top.trumeet.mipush.provider.entities.Event;
import top.trumeet.mipush.provider.event.EventType;
import top.trumeet.mipush.provider.event.type.TypeFactory;

@Aspect
public class MIPushEventProcessorAspect {
    private static final String TAG = MIPushEventProcessorAspect.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    static void recordEvent(EventType type) {
        RegisteredApplicationDb.registerApplication(type.getPkg());
        logger.d("insertEvent -> " + type);
        EventDb.insertEvent(Event.ResultType.OK, type);
    }

    @Around("execution(* com.xiaomi.push.service.MIPushEventProcessor.buildIntent(..))")
    public Intent buildIntent(final ProceedingJoinPoint joinPoint) throws Throwable {
        Intent intent = (Intent) joinPoint.proceed();
        return ignoreMessageIdAndMessageTypeExtra(intent);
    }

    @NonNull
    private Intent ignoreMessageIdAndMessageTypeExtra(Intent intent) {
        return new Intent(intent) {
            @NonNull
            @Override
            public Intent putExtra(String name, @Nullable String value) {
                if ("messageId".equals(name))
                    return this;
                return super.putExtra(name, value);
            }

            @NonNull
            @Override
            public Intent putExtra(String name, int value) {
                if (ReportConstants.EVENT_MESSAGE_TYPE.equals(name))
                    return this;
                return super.putExtra(name, value);
            }
        };
    }

    @Around("execution(* com.nihility.XMPushUtils.packToContainer(..))" +
            "|| execution(* com.xiaomi.push.service.MIPushEventProcessor.buildContainer(..))")
    public XmPushActionContainer buildContainerHook(final ProceedingJoinPoint joinPoint) throws Throwable {
        XmPushActionContainer container = (XmPushActionContainer) joinPoint.proceed();
        recordContainer(container);
        return container;
    }

    private static void recordContainer(XmPushActionContainer container) {
        if (container == null) {
            return;
        }
        Global.RegistrationRecorder().recordRegSec(container);
        XmPushActionContainer decorated = decoratedContainer(container.packageName, container);
        AppInfoUtilsAspect.setLastMetaInfo(decorated.metaInfo);
    }

    @Around("execution(* com.xiaomi.push.service.MIPushEventProcessor.isIntentAvailable(..))")
    public boolean isIntentAvailable(final ProceedingJoinPoint joinPoint) {
        return true;
    }


    /**
     * default behavior is
     * return true
     * unless
     * - metaInfo.EXTRA_PARAM_CHECK_ALIVE exists
     * - metaInfo.EXTRA_PARAM_AWAKE is false
     * - the target application is not running
     */
    public boolean shouldSendBroadcast(
            final ProceedingJoinPoint joinPoint,
            XMPushService pushService, String packageName,
            XmPushActionContainer container, PushMetaInfo metaInfo) throws Throwable {
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

    @Before("execution(* com.xiaomi.push.service.MIPushEventProcessor.processMIPushMessage(..)) && args(pushService, decryptedContent, packetBytesLen)")
    public void processMIPushMessage(final JoinPoint joinPoint,
                                     XMPushService pushService, byte[] decryptedContent, long packetBytesLen) {
        logger.d(joinPoint.getSignature());

        XmPushActionContainer buildContainer = XMPushUtils.packToContainer(decryptedContent);
        if (MiPushMessageDuplicateAspect.isMockMessage(buildContainer)) {
            return;
        }
        logger.d("buildContainer" + " " + ConvertUtils.toJson(buildContainer));
        Global.MiPushEventListener().receiveFromServer(buildContainer);
        recordEvent(TypeFactory.createForStore(buildContainer));
    }


    @Around("execution(* com.xiaomi.push.service.MIPushEventProcessor.postProcessMIPushMessage(..)) && args(pushService, pkgName, payload, newMessageIntent)")
    public void postProcessMIPushMessage(
            final ProceedingJoinPoint joinPoint,
            XMPushService pushService, String pkgName, byte[] payload, Intent newMessageIntent) throws Throwable {
        Global.HookHandler().postProcessMIPushMessage(joinPoint, pushService, pkgName, payload, newMessageIntent);
    }

    public static XmPushActionContainer decoratedContainer(String realTargetPackage, XmPushActionContainer container) {
        XmPushActionContainer decorated = container.deepCopy();
        try {
            Configurations.getInstance().handle(realTargetPackage, decorated);
        } catch (Throwable e) {
            // Ignore
        }
        return decorated;
    }

}
