package com.xiaomi.push.service;

import static com.xiaomi.push.service.MIPushEventProcessor.buildContainer;
import static com.xiaomi.push.service.MIPushEventProcessor.buildIntent;
import static com.xiaomi.push.service.MIPushEventProcessor.sendGeoAck;
import static com.xiaomi.push.service.MiPushMsgAck.geoMessageIsValidated;
import static com.xiaomi.push.service.MiPushMsgAck.processGeoMessage;
import static com.xiaomi.push.service.MiPushMsgAck.sendAckMessage;
import static com.xiaomi.push.service.MiPushMsgAck.sendAppAbsentAck;
import static com.xiaomi.push.service.MiPushMsgAck.sendAppNotInstallNotification;
import static com.xiaomi.push.service.MiPushMsgAck.sendErrorAck;
import static com.xiaomi.push.service.MiPushMsgAck.verifyGeoMessage;

import android.accounts.Account;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.channel.commonutils.android.AppInfoUtils;
import com.xiaomi.channel.commonutils.android.MIIDUtils;
import com.xiaomi.channel.commonutils.reflect.JavaCalls;
import com.xiaomi.mipush.sdk.PushContainerHelper;
import com.xiaomi.xmpush.thrift.ActionType;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmpush.thrift.XmPushActionRegistrationResult;
import com.xiaomi.xmsf.R;
import com.xiaomi.xmsf.push.utils.Configurations;

import java.util.Map;

import top.trumeet.common.cache.ApplicationNameCache;
import top.trumeet.common.utils.NotificationUtils;
import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.db.RegisteredApplicationDb;
import top.trumeet.mipush.provider.register.RegisteredApplication;


/**
 * @author zts1993
 * @date 2018/2/8
 */

public class MyMIPushMessageProcessor {
    private static Logger logger = XLog.tag("MyMIPushMessageProcessor").build();
    public static void processMIPushMessage(XMPushService pushService, byte[] decryptedContent) {
        processMIPushMessage(pushService, decryptedContent, false);
    }
    public static void processMIPushMessage(XMPushService pushService, byte[] decryptedContent, boolean moke) {
        try {
            XmPushActionContainer container = buildContainer(decryptedContent);
            if (container == null) {
                return;
            }
            if (TextUtils.isEmpty(container.packageName)) {
                logger.w("receive a mipush message without package name");
                return;
            }

            Long receiveTime = Long.valueOf(System.currentTimeMillis());
            Intent intent = buildIntent(decryptedContent, receiveTime.longValue());
            String realTargetPackage = MIPushNotificationHelper.getTargetPackage(container);
            PushMetaInfo metaInfo = container.getMetaInfo();
            if (metaInfo != null) {
                metaInfo.putToExtra(PushConstants.MESSAGE_RECEIVE_TIME, Long.toString(receiveTime.longValue()));
            }

            boolean isSendMessage = ActionType.SendMessage == container.getAction();
            boolean isBusinessMessage = MIPushNotificationHelper.isBusinessMessage(container);

            if (isSendMessage && MIPushAppInfo.getInstance(pushService).isUnRegistered(container.packageName) && !isBusinessMessage) {
                String msgId = "";
                if (metaInfo != null) {
                    msgId = metaInfo.getId();
                }
                logger.w("Drop a message for unregistered, msgid=" + msgId);
                sendAppAbsentAck(pushService, container, container.packageName);
            } else if (isSendMessage && MIPushAppInfo.getInstance(pushService).isPushDisabled4User(container.packageName) && !isBusinessMessage) {
                String msgId2 = "";
                if (metaInfo != null) {
                    msgId2 = metaInfo.getId();
                }
                logger.w("Drop a message for push closed, msgid=" + msgId2);
                sendAppAbsentAck(pushService, container, container.packageName);
            } else if (isSendMessage && !TextUtils.equals(pushService.getPackageName(), PushConstants.PUSH_SERVICE_PACKAGE_NAME) && !TextUtils.equals(pushService.getPackageName(), container.packageName)) {
                logger.w("Receive a message with wrong package name, expect " + pushService.getPackageName() + ", received " + container.packageName);
                sendErrorAck(pushService, container, "unmatched_package", "package should be " + pushService.getPackageName() + ", but got " + container.packageName);
            } else {
                if (metaInfo != null && metaInfo.getId() != null) {
                    logger.i(String.format("receive a message, appid=%s, msgid= %s", container.getAppid(), metaInfo.getId()));
                }

                if (metaInfo != null) {
                    Map<String, String> extra = metaInfo.getExtra();
                    if (extra != null && extra.containsKey("hide") && "true".equalsIgnoreCase(extra.get("hide"))) {
                        logger.i(String.format("hide a message, appid=%s, msgid= %s", container.getAppid(), metaInfo.getId()));
                        sendAckMessage(pushService, container);
                        return;
                    }
                }

                if (metaInfo != null && metaInfo.getExtra() != null && metaInfo.getExtra().containsKey(PushConstants.EXTRA_PARAM_MIID)) {
                    String miid = metaInfo.getExtra().get(PushConstants.EXTRA_PARAM_MIID);
                    Account miAccount = MIIDUtils.getXiaomiAccount(pushService);
                    String oldAccount = "";
                    if (miAccount == null) {
                        // xiaomi account login ?
                        oldAccount = "nothing";
                    } else {
                        if (!TextUtils.equals(miid, miAccount.name)) {
                            oldAccount = miAccount.name;
                            logger.w(miid + " should be login, but got " + miAccount);
                        }
                    }

                    if (!oldAccount.isEmpty()) {
                        logger.w("miid already logout or anther already login :" + oldAccount);
                        sendErrorAck(pushService, container, "miid already logout or anther already login", oldAccount);
                    }
                }

                boolean relatedToGeo = metaInfo != null && verifyGeoMessage(metaInfo.getExtra());
                if (relatedToGeo) {
                    if (geoMessageIsValidated(pushService, container)) {
                        boolean showNow = processGeoMessage(pushService, metaInfo, decryptedContent);
                        sendGeoAck(pushService, container, true, false, false);
                        if (!showNow) {
                            return;
                        }
                    } else {
                        return;
                    }
                }

                postProcessMIPushMessage(pushService, realTargetPackage, decryptedContent, intent, relatedToGeo, moke);
            }
        } catch (RuntimeException e2) {
            logger.e("fallbackProcessMIPushMessage failed at" + System.currentTimeMillis(), e2);
        }
    }


    /**
     * @see MIPushEventProcessor#postProcessMIPushMessage
     */
    private static void postProcessMIPushMessage(XMPushService pushService, String realTargetPackage, byte[] decryptedContent, Intent intent, boolean relateToGeo, boolean moke) {
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
                String pkgName = container.getPackageName();
                SharedPreferences sp = pushService.getSharedPreferences(PushServiceConstants.PREF_KEY_REGISTERED_PKGS, 0);
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(pkgName, container.appid);
                editor.commit();
                String regSec = null;

                try {
                    XmPushActionRegistrationResult result = (XmPushActionRegistrationResult) PushContainerHelper.getResponseMessageBodyFromContainer(pushService, container);
                    regSec = result.getRegSecret();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                Utils.setRegSec(pkgName, regSec);

                com.xiaomi.tinyData.TinyDataManager.getInstance(pushService).processPendingData("Register Success, package name is " + pkgName);
            }

            String title;
            String description;
            if (metaInfo != null) {
                title = metaInfo.getTitle();
                description = metaInfo.getDescription();

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

            RegisteredApplication application = RegisteredApplicationDb.registerApplication(
                    realTargetPackage, false);
            XmPushActionContainer decorated = container.deepCopy();
            try {
                Configurations.getInstance().handle(realTargetPackage, decorated);
            } catch (Throwable e) {
                // Ignore
            }
            boolean awake = Boolean.parseBoolean(
                    NotificationUtils.getExtraField(
                            decorated.metaInfo.getExtra(), PushConstants.EXTRA_PARAM_AWAKE, null));
            boolean isSystemApp = false;
            try {
                int flags = pushService.getPackageManager().getApplicationInfo(realTargetPackage, 0).flags &
                        (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP);
                isSystemApp = flags != 0;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if (metaInfo == null || TextUtils.isEmpty(metaInfo.getTitle()) || TextUtils.isEmpty(metaInfo.getDescription()) ||
                    (metaInfo.passThrough == 1 && !application.getShowPassThrough()) /* ||
                    (!MIPushNotificationHelper.isNotifyForeground(metaInfo.getExtra()) && MIPushNotificationHelper.isApplicationForeground(pushService, container.packageName)) */) {
                if (PushConstants.PUSH_SERVICE_PACKAGE_NAME.contains(container.packageName) &&
                        !container.isEncryptAction() && metaInfo != null && metaInfo.getExtra() != null &&
                        metaInfo.getExtra().containsKey("ab")) {
                    sendAckMessage(pushService, container);
                    logger.v("receive abtest message. ack it." + metaInfo.getId());
                } else if (awake || container.action != ActionType.SendMessage || isSystemApp) {
                    pushService.sendBroadcast(intent, ClientEventDispatcher.getReceiverPermission(container.packageName));
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
                if (isDupMessage && !moke) {
                    logger.w("drop a duplicate message, key=" + key);
                } else {
                    MyMIPushNotificationHelper.notifyPushMessage(pushService, decryptedContent);

                    if (awake || isSystemApp) {
                        if (metaInfo.passThrough == 1) {
                            pushService.sendBroadcast(intent, ClientEventDispatcher.getReceiverPermission(container.packageName));
                        } else if (!isBusinessMessage) {
                            Intent messageArrivedIntent = new Intent(PushConstants.MIPUSH_ACTION_MESSAGE_ARRIVED);
                            messageArrivedIntent.putExtra(PushConstants.MIPUSH_EXTRA_PAYLOAD, decryptedContent);
                            messageArrivedIntent.setPackage(container.packageName);
                            pushService.sendBroadcast(messageArrivedIntent, ClientEventDispatcher.getReceiverPermission(container.packageName));
                        }
                    }
                }

                if (relateToGeo) {
                    sendGeoAck(pushService, container, false, true, false);
                } else {
                    sendAckMessage(pushService, container);
                }
            }
            if (container.getAction() == ActionType.UnRegistration && !PushConstants.PUSH_SERVICE_PACKAGE_NAME.equals(pushService.getPackageName())) {
                pushService.stopSelf();
            }
        }
    }


}
