package com.xiaomi.push.service;

import static com.xiaomi.push.service.MIPushEventProcessor.buildContainer;
import static com.xiaomi.push.service.MiPushMsgAck.sendAckMessage;
import static com.xiaomi.push.service.MiPushMsgAck.sendAppNotInstallNotification;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.nihility.service.RegistrationRecorder;
import com.xiaomi.channel.commonutils.android.AppInfoUtils;
import com.xiaomi.channel.commonutils.reflect.JavaCalls;
import com.xiaomi.push.service.clientReport.ReportConstants;
import com.xiaomi.xmpush.thrift.ActionType;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmpush.thrift.XmPushThriftSerializeUtils;
import com.xiaomi.xmsf.R;
import com.xiaomi.xmsf.push.type.TypeFactory;
import com.xiaomi.xmsf.push.utils.Configurations;
import com.xiaomi.xmsf.utils.ConvertUtils;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import top.trumeet.common.cache.ApplicationNameCache;
import top.trumeet.common.utils.CustomConfiguration;
import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.db.EventDb;
import top.trumeet.mipush.provider.db.RegisteredApplicationDb;
import top.trumeet.mipush.provider.event.Event;
import top.trumeet.mipush.provider.event.EventType;
import top.trumeet.mipush.provider.register.RegisteredApplication;

@Aspect
public class MIPushEventProcessorAspect {
    private static final String TAG = MIPushEventProcessorAspect.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();
    public static String mockFlag = "__mock__";

    static boolean userAllow(EventType type, Context context) {
        RegisteredApplication application = RegisteredApplicationDb.registerApplication(type.getPkg(),
                true);
        if (application == null) {
            return false;
        }
        logger.d("insertEvent -> " + type);
        EventDb.insertEvent(Event.ResultType.OK, type);
        return true;
    }

    public static void mockProcessMIPushMessage(XMPushService pushService, byte[] decryptedContent) {
        XmPushActionContainer container = buildContainer(decryptedContent);
        mockProcessMIPushMessage(pushService, container);
    }

    public static void mockProcessMIPushMessage(XMPushService pushService, XmPushActionContainer container) {
        try {
            if (container != null) {
                PushMetaInfo metaInfo = container.getMetaInfo();
                if (metaInfo != null) {
                    metaInfo.putToExtra(mockFlag, Boolean.toString(true));
                    byte[] mockDecryptedContent = XmPushThriftSerializeUtils.convertThriftObjectToBytes(container);
                    JavaCalls.<Boolean>callStaticMethodOrThrow(MIPushEventProcessor.class.getName(), "processMIPushMessage",
                            pushService, mockDecryptedContent, (long) mockDecryptedContent.length);
                }
            }
        } catch (Exception e) {
            logger.e("mock notification failure: ", e);
            Utils.makeText(pushService, "failure", Toast.LENGTH_SHORT);
        }
    }

    static boolean isMockMessage(XmPushActionContainer container) {
        if (container != null) {
            PushMetaInfo metaInfo = container.getMetaInfo();
            if (metaInfo != null) {
                return metaInfo.getExtra() != null && Boolean.parseBoolean(metaInfo.getExtra().get(mockFlag));
            }
        }
        return false;
    }

    @Around("execution(* com.xiaomi.push.service.MIPushEventProcessor.buildIntent(..))")
    public Intent buildIntent(final ProceedingJoinPoint joinPoint) throws Throwable {
        Intent intent = (Intent) joinPoint.proceed();
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
        RegistrationRecorder.getInstance().recordRegSec(container);
        return container;
    }

    @Around("execution(* com.xiaomi.push.service.MIPushEventProcessor.shouldSendBroadcast(..)) && args(pushService, packageName, container, metaInfo)")
    public boolean shouldSendBroadcast(final ProceedingJoinPoint joinPoint,
                                       XMPushService pushService, String packageName, XmPushActionContainer container, PushMetaInfo metaInfo) throws Throwable {
        joinPoint.proceed();
        return checkAwakeField(metaInfo);
    }

    public static boolean checkAwakeField(PushMetaInfo metaInfo) {
        boolean extraExists = metaInfo != null && metaInfo.extra != null;
        if (extraExists) {
            String awakeField = metaInfo.extra.get(PushConstants.EXTRA_PARAM_AWAKE);
            return Boolean.parseBoolean(awakeField);
        }
        return false;
    }

    @Before("execution(* com.xiaomi.push.service.MIPushEventProcessor.processMIPushMessage(..)) && args(pushService, decryptedContent, packetBytesLen)")
    public void processMIPushMessage(final JoinPoint joinPoint,
                                     XMPushService pushService, byte[] decryptedContent, long packetBytesLen) {
        logger.d(joinPoint.getSignature());

        XmPushActionContainer buildContainer = buildContainer(decryptedContent);
        if (isMockMessage(buildContainer)) {
            return;
        }
        logger.d("buildContainer" + " " + ConvertUtils.toJson(buildContainer));
        EventType type = TypeFactory.create(buildContainer, buildContainer.packageName);
        userAllow(type, pushService);
    }

    @Around("execution(* com.xiaomi.push.service.MIPushEventProcessor.postProcessMIPushMessage(..)) && args(pushService, realTargetPackage, decryptedContent, intent)")
    public void postProcessMIPushMessage(final ProceedingJoinPoint joinPoint,
                                         XMPushService pushService, String realTargetPackage, byte[] decryptedContent, Intent intent) {
        logger.d(joinPoint.getSignature());

        XmPushActionContainer container = buildContainer(decryptedContent);
        PushMetaInfo metaInfo = container.getMetaInfo();

        boolean isBusinessMessage = MIPushNotificationHelper.isBusinessMessage(container);
        boolean pkgInstalled = AppInfoUtils.isPkgInstalled(pushService, container.packageName);

        if (JavaCalls.<Boolean>callStaticMethod(MIPushEventProcessor.class.getName(), "isMIUIOldAdsSDKMessage", container) &&
                JavaCalls.<Boolean>callStaticMethod(MIPushEventProcessor.class.getName(), "isMIUIPushSupported", pushService, realTargetPackage)) {
            JavaCalls.callStaticMethod(MIPushEventProcessor.class.getName(), "sendMIUIOldAdsAckMessage", pushService, container);
        } else if (JavaCalls.<Boolean>callStaticMethod(MIPushEventProcessor.class.getName(), "isMIUIPushMessage", container) &&
                !JavaCalls.<Boolean>callStaticMethod(MIPushEventProcessor.class.getName(), "isMIUIPushSupported", pushService, realTargetPackage) &&
                !JavaCalls.<Boolean>callStaticMethod(MIPushEventProcessor.class.getName(), "predefinedNotification", container)) {
            JavaCalls.callStaticMethod(MIPushEventProcessor.class.getName(), "sendMIUINewAdsAckMessage", pushService, container);
        } else {
            if (!pkgInstalled) {
                sendAppNotInstallNotification(pushService, container);
                return;
            }
            if (ActionType.Registration == container.getAction()) {
                intent.putExtra(ReportConstants.EVENT_MESSAGE_TYPE, ReportConstants.REGISTER_TYPE);
                String pkgName = container.getPackageName();
                SharedPreferences sp = pushService.getSharedPreferences(PushServiceConstants.PREF_KEY_REGISTERED_PKGS, 0);
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(pkgName, container.appid);
                editor.commit();
                MIPushAppInfo.getInstance(pushService).removeDisablePushPkg(pkgName);
                MIPushAppInfo.getInstance(pushService).removeDisablePushPkgCache(pkgName);

                RegistrationRecorder.getInstance().initContext(pushService);
                RegistrationRecorder.getInstance().recordRegSec(container);
            }

            // todo: delete tracking code {
            if (!TextUtils.isEmpty(metaInfo.getId())) {
                intent.putExtra("messageId", metaInfo.getId());
            }
            if (MIPushNotificationHelper.isNormalNotificationMessage(container)) {
                intent.putExtra(ReportConstants.EVENT_MESSAGE_TYPE, 1000);
            }
            if (MIPushNotificationHelper.isPassThoughMessage(container)) {
                intent.putExtra(ReportConstants.EVENT_MESSAGE_TYPE, 2000);
            }
            if (MIPushNotificationHelper.isBusinessMessage(container)) {
                intent.putExtra(ReportConstants.EVENT_MESSAGE_TYPE, ReportConstants.AWAKE_TYPE);
            }
            // }

            String title;
            String description;
            if (metaInfo != null) {
                title = metaInfo.getTitle();
                description = metaInfo.getDescription();

                completeTitleOrDescriptionIfExistsAnyone(pushService, realTargetPackage, title, description, metaInfo);
            }

            RegisteredApplication application = RegisteredApplicationDb.registerApplication(
                    realTargetPackage, false);
            XmPushActionContainer decorated = decoratedContainer(realTargetPackage, container);
            CustomConfiguration configuration = new CustomConfiguration(decorated.metaInfo.getExtra());
            boolean awake = configuration.get(PushConstants.EXTRA_PARAM_AWAKE, false);
            boolean isSystemApp = isSystemApp(pushService, realTargetPackage);
            if (metaInfo == null || TextUtils.isEmpty(metaInfo.getTitle()) || TextUtils.isEmpty(metaInfo.getDescription()) ||
                    decorated.metaInfo.passThrough == 1 /* ||
                    (!MIPushNotificationHelper.isNotifyForeground(metaInfo.getExtra()) && MIPushNotificationHelper.isApplicationForeground(pushService, container.packageName)) */) {
                if (PushConstants.PUSH_SERVICE_PACKAGE_NAME.contains(container.packageName) &&
                        !container.isEncryptAction() && metaInfo != null && metaInfo.getExtra() != null &&
                        metaInfo.getExtra().containsKey("ab")) {
                    sendAckMessage(pushService, container);
                    logger.v("receive abtest message. ack it." + metaInfo.getId());
                } else if (awake || container.action != ActionType.SendMessage || isSystemApp) {
                    pushService.sendBroadcast(intent, MIPushHelper.getReceiverPermission(container.packageName));
                }
            } else {
                String key = null;
                if (metaInfo.extra != null) {
                    key = metaInfo.extra.get("jobkey");
                }
                if (TextUtils.isEmpty(key)) {
                    key = metaInfo.getId();
                }
                boolean isDupMessage = MiPushMessageDuplicate.isDuplicateMessage(pushService, container.packageName, key);
                if (isDupMessage && !isMockMessage(container)) {
                    logger.w("drop a duplicate message, key=" + key);
                } else {
                    MyMIPushNotificationHelper.notifyPushMessage(pushService, decryptedContent);

                    if (awake || isSystemApp) {
                        if (decorated.metaInfo.passThrough == 1) {
                            pushService.sendBroadcast(intent, MIPushHelper.getReceiverPermission(container.packageName));
                        } else if (!isBusinessMessage) {
                            Intent messageArrivedIntent = new Intent(PushConstants.MIPUSH_ACTION_MESSAGE_ARRIVED);
                            messageArrivedIntent.putExtra(PushConstants.MIPUSH_EXTRA_PAYLOAD, decryptedContent);
                            messageArrivedIntent.setPackage(container.packageName);
                            pushService.sendBroadcast(messageArrivedIntent, MIPushHelper.getReceiverPermission(container.packageName));
                        }
                    }
                }
                sendAckMessage(pushService, container);
            }
            if (container.getAction() == ActionType.UnRegistration && !PushConstants.PUSH_SERVICE_PACKAGE_NAME.equals(pushService.getPackageName())) {
                pushService.stopSelf();
            }
        }
    }

    private static boolean isSystemApp(XMPushService pushService, String realTargetPackage) {
        boolean isSystemApp = false;
        try {
            int flags = pushService.getPackageManager().getApplicationInfo(realTargetPackage, 0).flags &
                    (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP);
            isSystemApp = flags != 0;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return isSystemApp;
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

    private static void completeTitleOrDescriptionIfExistsAnyone(XMPushService pushService, String realTargetPackage, String title, String description, PushMetaInfo metaInfo) {
        if (!TextUtils.isEmpty(title) || !TextUtils.isEmpty(description)) {
            if (TextUtils.isEmpty(title)) {
                CharSequence appName = ApplicationNameCache.getInstance().getAppName(pushService, realTargetPackage);
                metaInfo.setTitle(appName.toString());
            }

            if (TextUtils.isEmpty(description)) {
                metaInfo.setDescription(pushService.getString(R.string.see_pass_though_msg));
            }
        }
    }

}
