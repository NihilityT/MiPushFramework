package com.xiaomi.push.service;

import static com.xiaomi.push.service.MIPushNotificationHelper.FROM_NOTIFICATION;
import static com.xiaomi.push.service.MIPushNotificationHelper.getTargetPackage;
import static com.xiaomi.push.service.MIPushNotificationHelper.isBusinessMessage;
import static top.trumeet.common.utils.NotificationUtils.getExtraField;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.channel.commonutils.android.AppInfoUtils;
import com.xiaomi.channel.commonutils.reflect.JavaCalls;
import com.xiaomi.push.sdk.MyPushMessageHandler;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmsf.BuildConfig;
import com.xiaomi.xmsf.R;
import com.xiaomi.xmsf.push.notification.NotificationController;
import com.xiaomi.xmsf.push.utils.Configurations;
import com.xiaomi.xmsf.utils.ConfigCenter;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import top.trumeet.common.Constants;
import top.trumeet.common.db.RegisteredApplicationDb;
import top.trumeet.common.register.RegisteredApplication;
import top.trumeet.common.utils.CustomNotifyIcon;
import top.trumeet.common.utils.Utils;

/**
 * @author zts1993
 * @date 2018/2/8
 */

public class MyMIPushNotificationHelper {
    public static final String CLASS_NAME_PUSH_MESSAGE_HANDLER = "com.xiaomi.mipush.sdk.PushMessageHandler";
    private static Logger logger = XLog.tag("MyNotificationHelper").build();

    private static final int NOTIFICATION_BIG_STYLE_MIN_LEN = 25;

    private static final String GROUP_TYPE_MIPUSH_GROUP = "#group#";
    private static final String GROUP_TYPE_SAME_TITLE = "#title#";
    private static final String GROUP_TYPE_SAME_NOTIFICATION_ID = "#id#";
    private static final String GROUP_TYPE_PASS_THROUGH = "#pass_through#";

    private static boolean tryLoadConfigurations = false;

    /**
     * @see MIPushNotificationHelper#notifyPushMessage
     */
    public static void notifyPushMessage(Context context, XmPushActionContainer container, byte[] decryptedContent) {
        AppInfoUtils.AppNotificationOp notificationOp = AppInfoUtils.getAppNotificationOp(context, getTargetPackage(container));
        if (notificationOp == AppInfoUtils.AppNotificationOp.NOT_ALLOWED) {
            logger.w("Do not notify because user block " + getTargetPackage(container) + "'s notification");
        } else if (TypedShieldHelper.isShield(context, container)) {
            String shieldTypeName = TypedShieldHelper.getShieldType(container);
            logger.w("Do not notify because user block " + shieldTypeName + "'s notification");
        } else {

            PushMetaInfo metaInfo = container.getMetaInfo();
            String packageName = container.getPackageName();

            if (!tryLoadConfigurations) {
                tryLoadConfigurations = true;
                boolean success = false;
                try {
                    success =
                            Configurations.getInstance().init(context,
                            ConfigCenter.getInstance().getConfigurationDirectory(context))
                            && CustomNotifyIcon.getInstance().init(context,
                            ConfigCenter.getInstance().getCustomNotifyIconPath(context));
                } catch (Exception e) {
                    Utils.makeText(context, e.toString(), Toast.LENGTH_LONG);
                }
                Utils.makeText(context, "configurations loaded: " + success, Toast.LENGTH_SHORT);
            }

            try {
                Set<String> operations = Configurations.getInstance().handle(packageName, metaInfo);

                if (operations.contains(Configurations.PackageConfig.OPERATION_WAKE)) {
                    wakeScreen(context, packageName);
                }
                if (!operations.contains(Configurations.PackageConfig.OPERATION_IGNORE)) {
                    doNotifyPushMessage(context, container, decryptedContent);
                }
                if (operations.contains(Configurations.PackageConfig.OPERATION_OPEN)) {
                    MyPushMessageHandler.startService(context, container, decryptedContent);
                }
            } catch (Exception e) {
                logger.e(e.getLocalizedMessage(), e);
            }
        }
    }

    private static void wakeScreen(Context context, String sourcePackage) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock fullWakeLock = powerManager.newWakeLock((
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                        PowerManager.FULL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP
        ), "xmsf: configurations of " + sourcePackage);
        fullWakeLock.acquire(10000);
    }

    private static void doNotifyPushMessage(Context context, XmPushActionContainer container, byte[] decryptedContent) {
        PushMetaInfo metaInfo = container.getMetaInfo();
        String packageName = container.getPackageName();

        String title = metaInfo.getTitle();
        String description = metaInfo.getDescription();

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);

        logger.i("title:" + title + "  description:" + description);

        if (description.length() > NOTIFICATION_BIG_STYLE_MIN_LEN) {
            NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
            style.bigText(description);
            style.setBigContentTitle(title);
            style.setSummaryText(description);
            notificationBuilder.setStyle(style);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            try {
//                RemoteViews localRemoteViews = JavaCalls.callStaticMethodOrThrow(MIPushNotificationHelper.class, "getNotificationForCustomLayout", var0.getApplicationContext(), buildContainer, var1);
//                if (localRemoteViews != null) {
//                    localBuilder.setCustomContentView(localRemoteViews);
//                }
//            } catch (Exception e) {
//                logger.e(e.getLocalizedMessage(), e);
//            }
        }

        addDebugAction(context, container, decryptedContent, metaInfo, packageName, notificationBuilder);

        notificationBuilder.setWhen(metaInfo.getMessageTs());
        notificationBuilder.setShowWhen(true);

        String[] titleAndDesp = determineTitleAndDespByDIP(context, metaInfo);
        notificationBuilder.setContentTitle(titleAndDesp[0]);
        notificationBuilder.setContentText(titleAndDesp[1]);

        String group = getGroupName(context, container);
        notificationBuilder.setGroup(group);

        RegisteredApplication application = RegisteredApplicationDb.registerApplication(
                packageName, false, context, null);
        boolean isGroupOfSession = application.isGroupNotificationsForSameSession();


        int notificationId = application.isGroupNotificationsByTitle() ?
                (container.packageName + "_" + metaInfo.title).hashCode() :
                MyClientEventDispatcher.getNotificationId(context, container);
        if (isGroupOfSession) {
            notificationId = (notificationId + "_" + System.currentTimeMillis()).hashCode();
        }

        Intent intentExtra = new Intent();
        intentExtra.putExtra(Constants.INTENT_NOTIFICATION_ID, notificationId);
        intentExtra.putExtra(Constants.INTENT_NOTIFICATION_GROUP, notificationBuilder.build().getGroup());
        intentExtra.putExtra(Constants.INTENT_NOTIFICATION_GROUP_OF_SESSION, isGroupOfSession);

        PendingIntent localPendingIntent = getClickedPendingIntent(
                context, container, decryptedContent, notificationId, intentExtra.getExtras());
        if (localPendingIntent != null) {
            notificationBuilder.setContentIntent(localPendingIntent);
            carryPendingIntentForTemporarilyWhitelisted(context, container, notificationBuilder);
        }

        NotificationController.publish(context, metaInfo, notificationId, packageName, notificationBuilder);
    }

    private static void carryPendingIntentForTemporarilyWhitelisted(Context xmPushService, XmPushActionContainer buildContainer, NotificationCompat.Builder localBuilder) {
        PushMetaInfo metaInfo = buildContainer.getMetaInfo();
        // Also carry along the target PendingIntent, whose target will get temporarily whitelisted for background-activity-start upon sent.
        final Intent targetIntent = buildTargetIntentWithoutExtras(buildContainer.getPackageName(), metaInfo);
        final PendingIntent pi = PendingIntent.getService(xmPushService, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        localBuilder.getExtras().putParcelable("mipush.target", pi);
    }

    private static String getGroupName(Context xmPushService, XmPushActionContainer buildContainer) {
        PushMetaInfo metaInfo = buildContainer.getMetaInfo();
        String packageName = buildContainer.getPackageName();
        RegisteredApplication application = RegisteredApplicationDb.registerApplication(
                packageName, false, xmPushService, null);

        boolean groupSession = application != null && application.isGroupNotificationsForSameSession();
        String group = getExtraField(metaInfo.getExtra(), "notification_group", null);
        if (group != null) {
            group = packageName + "_" + GROUP_TYPE_MIPUSH_GROUP + "_" + group;
        } else if (groupSession && application.isGroupNotificationsByTitle()) {
            group = packageName + "_" + GROUP_TYPE_SAME_TITLE + "_" + metaInfo.getTitle().hashCode();
        } else if (metaInfo.passThrough == 1) {
            group = packageName + "_" + GROUP_TYPE_PASS_THROUGH;
        } else if (groupSession) {
            String id = metaInfo.isSetNotifyId() ? String.valueOf(metaInfo.getNotifyId()) : "";
            group = packageName + "_" + GROUP_TYPE_SAME_NOTIFICATION_ID + "_" + id;
        } else {
            group = packageName;
        }
        return group;
    }

    private static void addDebugAction(Context xmPushService, XmPushActionContainer buildContainer, byte[] var1, PushMetaInfo metaInfo, String packageName, NotificationCompat.Builder localBuilder) {
        if (BuildConfig.DEBUG) {
            int i = R.drawable.ic_notifications_black_24dp;

            PendingIntent pendingIntentOpenActivity = openActivityPendingIntent(xmPushService, buildContainer, metaInfo, var1);
            if (pendingIntentOpenActivity != null) {
                localBuilder.addAction(new NotificationCompat.Action(i, "Open App", pendingIntentOpenActivity));
            }

            PendingIntent pendingIntentJump = startServicePendingIntent(xmPushService, buildContainer, metaInfo, var1);
            if (pendingIntentJump != null) {
                localBuilder.addAction(new NotificationCompat.Action(i, "Jump", pendingIntentJump));
            }

            Intent sdkIntentJump = getSdkIntent(xmPushService, packageName, buildContainer);
            if (sdkIntentJump != null) {
                PendingIntent pendingIntent = PendingIntent.getActivity(xmPushService, 0, sdkIntentJump, PendingIntent.FLAG_UPDATE_CURRENT);
                localBuilder.addAction(new NotificationCompat.Action(i, "SDK Intent", pendingIntent));
            }
        }
    }

    public static Intent buildTargetIntentWithoutExtras(final String pkg, final PushMetaInfo metaInfo) {
        return new Intent(PushConstants.MIPUSH_ACTION_NEW_MESSAGE).addCategory(String.valueOf(metaInfo.getNotifyId()))
                .setClassName(pkg, CLASS_NAME_PUSH_MESSAGE_HANDLER);
    }

    private static PendingIntent openActivityPendingIntent(Context paramContext, XmPushActionContainer paramXmPushActionContainer, PushMetaInfo paramPushMetaInfo, byte[] paramArrayOfByte) {
        String packageName = paramXmPushActionContainer.getPackageName();
        PackageManager packageManager = paramContext.getPackageManager();
        Intent localIntent1 = packageManager.getLaunchIntentForPackage(packageName);
        if (localIntent1 != null) {
            localIntent1.addCategory(String.valueOf(paramPushMetaInfo.getNotifyId()));
            return PendingIntent.getActivity(paramContext, 0, localIntent1, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return null;
    }

    private static PendingIntent getClickedPendingIntent(
            Context context, XmPushActionContainer container, byte[] decryptedContent,
            int notificationId, Bundle extra) {
        PushMetaInfo metaInfo = container.getMetaInfo();
        if (metaInfo == null) {
            return null;
        }

        //Jump web
        String urlJump = null;
        if (!TextUtils.isEmpty(metaInfo.url)) {
            urlJump = metaInfo.url;
        } else if (metaInfo.getExtra() != null) {
            urlJump = metaInfo.getExtra().get(PushConstants.EXTRA_PARAM_WEB_URI);
        }

        if (!TextUtils.isEmpty(urlJump)) {
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setData(Uri.parse(urlJump));
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            return PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.xiaomi.xmsf",
                isBusinessMessage(container) ?
                        "com.xiaomi.mipush.sdk.PushMessageHandler" :
                        "com.xiaomi.push.sdk.MyPushMessageHandler"));
        intent.putExtra(PushConstants.MIPUSH_EXTRA_PAYLOAD, decryptedContent);
        intent.putExtra(FROM_NOTIFICATION, true);
        intent.putExtras(extra);
        intent.addCategory(String.valueOf(metaInfo.getNotifyId()));
        return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * @see com.xiaomi.mipush.sdk.PushMessageProcessor#getNotificationMessageIntent
     */
    public static Intent getSdkIntent(Context context, String pkgName, XmPushActionContainer container) {
        PushMetaInfo paramPushMetaInfo = container.getMetaInfo();
        Map<String, String> extra = paramPushMetaInfo.getExtra();
        if (extra == null) {
            return null;
        }

        if (!extra.containsKey(PushConstants.EXTRA_PARAM_NOTIFY_EFFECT)) {
            return null;
        }

        Intent intent = null;

        String typeId = extra.get(PushConstants.EXTRA_PARAM_NOTIFY_EFFECT);
        if (PushConstants.NOTIFICATION_CLICK_DEFAULT.equals(typeId)) {
            try {
                intent = context.getPackageManager().getLaunchIntentForPackage(pkgName);
            } catch (Exception e2) {
                logger.e("Cause: " + e2.getMessage());
            }
        } else if (PushConstants.NOTIFICATION_CLICK_INTENT.equals(typeId)) {

            if (extra.containsKey(PushConstants.EXTRA_PARAM_INTENT_URI)) {
                String intentStr = extra.get(PushConstants.EXTRA_PARAM_INTENT_URI);
                if (intentStr != null) {
                    try {
                        intent = Intent.parseUri(intentStr, Intent.URI_INTENT_SCHEME);
                        intent.setPackage(pkgName);
                    } catch (URISyntaxException e3) {
                        logger.e("Cause: " + e3.getMessage());
                    }
                }
            } else {
                if (extra.containsKey(PushConstants.EXTRA_PARAM_CLASS_NAME)) {
                    String className = (String) extra.get(PushConstants.EXTRA_PARAM_CLASS_NAME);
                    intent = new Intent();
                    intent.setComponent(new ComponentName(pkgName, className));
                    try {
                        if (extra.containsKey(PushConstants.EXTRA_PARAM_INTENT_FLAG)) {
                            intent.setFlags(Integer.parseInt(extra.get(PushConstants.EXTRA_PARAM_INTENT_FLAG)));
                        }
                    } catch (NumberFormatException e4) {
                        logger.e("Cause by intent_flag: " + e4.getMessage());
                    }

                }
            }
        } else if (PushConstants.NOTIFICATION_CLICK_WEB_PAGE.equals(typeId)) {
            String uri = extra.get(PushConstants.EXTRA_PARAM_WEB_URI);

            MalformedURLException e;

            if (uri != null) {
                String tmp = uri.trim();
                if (!(tmp.startsWith("http://") || tmp.startsWith("https://"))) {
                    tmp = "http://" + tmp;
                }
                try {
                    String protocol = new URL(tmp).getProtocol();
                    if (!"http".equals(protocol)) {
                        if (!"https".equals(protocol)) {
                            //why ?
                        }
                    }
                    Intent intent2 = new Intent("android.intent.action.VIEW");
                    intent2.setData(Uri.parse(tmp));
                    intent = intent2;
                } catch (MalformedURLException e6) {
                    e = e6;
                    logger.e("Cause: " + e.getMessage());
                    return null;
                }
            }
        }


        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                //TODO fixit

                //we don't have RegSecret we cannot decode push action

                if (inFetchIntentBlackList(pkgName)) {
                    return null;
                }

                return intent;
            }
        }

        return null;
    }

    /**
     * tmp black list
     *
     * @param pkg package name
     * @return is in black list
     */
    private static boolean inFetchIntentBlackList(String pkg) {
        if (pkg.contains("youku")) {
            return true;
        }

        return false;
    }


    private static PendingIntent startServicePendingIntent(Context paramContext, XmPushActionContainer paramXmPushActionContainer, PushMetaInfo paramPushMetaInfo, byte[] paramArrayOfByte) {
        if (paramPushMetaInfo == null) {
            return null;
        }

        Intent localIntent;
        if (isBusinessMessage(paramXmPushActionContainer)) {
            localIntent = new Intent();
            localIntent.setComponent(new ComponentName("com.xiaomi.xmsf", "com.xiaomi.mipush.sdk.PushMessageHandler"));
            localIntent.putExtra(PushConstants.MIPUSH_EXTRA_PAYLOAD, paramArrayOfByte);
            localIntent.putExtra(FROM_NOTIFICATION, true);
            localIntent.addCategory(String.valueOf(paramPushMetaInfo.getNotifyId()));
        } else {
            localIntent = new Intent(PushConstants.MIPUSH_ACTION_NEW_MESSAGE);
            localIntent.setComponent(new ComponentName(paramXmPushActionContainer.packageName, "com.xiaomi.mipush.sdk.PushMessageHandler"));
            localIntent.putExtra(PushConstants.MIPUSH_EXTRA_PAYLOAD, paramArrayOfByte);
            localIntent.putExtra(FROM_NOTIFICATION, true);
            localIntent.addCategory(String.valueOf(paramPushMetaInfo.getNotifyId()));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return PendingIntent.getForegroundService(paramContext, 0, localIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            return PendingIntent.getService(paramContext, 0, localIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    /**
     * @see MIPushNotificationHelper#determineTitleAndDespByDIP
     */
    private static String[] determineTitleAndDespByDIP(Context paramContext, PushMetaInfo paramPushMetaInfo) {

        try {
            return JavaCalls.callStaticMethodOrThrow(MIPushNotificationHelper.class, "determineTitleAndDespByDIP", paramContext, paramPushMetaInfo);
        } catch (Exception e) {
            logger.e(e.getMessage(), e);
            return new String[]{paramPushMetaInfo.getTitle(), paramPushMetaInfo.getDescription()};
        }
    }


}
