package com.xiaomi.push.service;

import static com.xiaomi.push.service.MIPushEventProcessor.buildContainer;

import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.com.xiaomi.channel.commonutils.android.AppInfoUtilsAspect;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.nihility.service.RegistrationRecorder;
import com.xiaomi.channel.commonutils.reflect.JavaCalls;
import com.xiaomi.push.service.clientReport.ReportConstants;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmpush.thrift.XmPushThriftSerializeUtils;
import com.xiaomi.xmsf.push.type.TypeFactory;
import com.xiaomi.xmsf.push.utils.Configurations;
import com.xiaomi.xmsf.utils.ConvertUtils;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import java.lang.reflect.InvocationTargetException;

import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.db.EventDb;
import top.trumeet.mipush.provider.db.RegisteredApplicationDb;
import top.trumeet.mipush.provider.event.Event;
import top.trumeet.mipush.provider.event.EventType;

@Aspect
public class MIPushEventProcessorAspect {
    private static final String TAG = MIPushEventProcessorAspect.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    static void recordEvent(EventType type) {
        RegisteredApplicationDb.registerApplication(type.getPkg());
        logger.d("insertEvent -> " + type);
        EventDb.insertEvent(Event.ResultType.OK, type);
    }

    public static void mockProcessMIPushMessage(XMPushService pushService, XmPushActionContainer container) {
        try {
            MiPushMessageDuplicateAspect.markAsMock(container);
            invokeProcessMiPushMessage(pushService, container);
        } catch (Exception e) {
            logger.e("mock notification failure: ", e);
            Utils.makeText(pushService, "failure", Toast.LENGTH_SHORT);
        }
    }

    private static void invokeProcessMiPushMessage(XMPushService pushService, XmPushActionContainer container) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        byte[] mockDecryptedContent = XmPushThriftSerializeUtils.convertThriftObjectToBytes(container);
        invokeProcessMiPushMessage(pushService, mockDecryptedContent);
    }

    private static void invokeProcessMiPushMessage(XMPushService pushService, byte[] mockDecryptedContent) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        JavaCalls.<Boolean>callStaticMethodOrThrow(MIPushEventProcessor.class.getName(), "processMIPushMessage",
                pushService, mockDecryptedContent, (long) mockDecryptedContent.length);
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

    @Around("execution(* com.xiaomi.push.service.MIPushEventProcessor.buildContainer(..))")
    public XmPushActionContainer buildContainerHook(final ProceedingJoinPoint joinPoint) throws Throwable {
        XmPushActionContainer container = (XmPushActionContainer) joinPoint.proceed();
        recordContainer(container);
        return container;
    }

    private static void recordContainer(XmPushActionContainer container) {
        if (container == null) {
            return;
        }
        RegistrationRecorder.getInstance().recordRegSec(container);
        XmPushActionContainer decorated = decoratedContainer(container.packageName, container);
        AppInfoUtilsAspect.setLastMetaInfo(decorated.metaInfo);
    }

    @Around("execution(* com.xiaomi.push.service.MIPushEventProcessor.isIntentAvailable(..))")
    public boolean isIntentAvailable(final ProceedingJoinPoint joinPoint) {
        return true;
    }


    @Around("execution(* com.xiaomi.push.service.MIPushEventProcessor.shouldSendBroadcast(..)) && args(pushService, packageName, container, metaInfo)")
    public boolean shouldSendBroadcast(final ProceedingJoinPoint joinPoint,
                                       XMPushService pushService, String packageName, XmPushActionContainer container, PushMetaInfo metaInfo) throws Throwable {
        joinPoint.proceed();
        XmPushActionContainer decorated = decoratedContainer(container.packageName, container);
        return AppInfoUtilsAspect.checkAwakeField(decorated.metaInfo);
    }

    @Before("execution(* com.xiaomi.push.service.MIPushEventProcessor.processMIPushMessage(..)) && args(pushService, decryptedContent, packetBytesLen)")
    public void processMIPushMessage(final JoinPoint joinPoint,
                                     XMPushService pushService, byte[] decryptedContent, long packetBytesLen) {
        logger.d(joinPoint.getSignature());

        XmPushActionContainer buildContainer = buildContainer(decryptedContent);
        if (MiPushMessageDuplicateAspect.isMockMessage(buildContainer)) {
            return;
        }
        logger.d("buildContainer" + " " + ConvertUtils.toJson(buildContainer));
        recordEvent(TypeFactory.create(buildContainer, buildContainer.packageName));
    }

    private static XmPushActionContainer decoratedContainer(String realTargetPackage, XmPushActionContainer container) {
        XmPushActionContainer decorated = container.deepCopy();
        try {
            Configurations.getInstance().handle(realTargetPackage, decorated);
        } catch (Throwable e) {
            // Ignore
        }
        return decorated;
    }

}
