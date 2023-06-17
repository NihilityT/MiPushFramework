package com.xiaomi.push.service;

import static com.xiaomi.push.service.MIPushEventProcessor.buildContainer;
import static com.xiaomi.push.service.MIPushNotificationHelper.FROM_NOTIFICATION;
import static com.xiaomi.push.service.MIPushNotificationHelper.getTargetPackage;
import static com.xiaomi.push.service.MIPushNotificationHelper.isBusinessMessage;
import static com.xiaomi.push.service.MyNotificationIconHelper.MiB;
import static com.xiaomi.xmsf.push.notification.NotificationController.getBitmapFromUri;
import static com.xiaomi.xmsf.push.notification.NotificationController.getLargeIcon;
import static com.xiaomi.xmsf.push.notification.NotificationController.getNotificationManagerEx;
import static top.trumeet.common.utils.NotificationUtils.getExtraField;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.nihility.notification.NotificationManagerEx;
import com.xiaomi.channel.commonutils.android.AppInfoUtils;
import com.xiaomi.channel.commonutils.reflect.JavaCalls;
import com.xiaomi.push.sdk.MyPushMessageHandler;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmsf.R;
import com.xiaomi.xmsf.push.notification.NotificationController;
import com.xiaomi.xmsf.push.utils.Configurations;
import com.xiaomi.xmsf.push.utils.IconConfigurations;
import com.xiaomi.xmsf.push.utils.PackageConfig;
import com.xiaomi.xmsf.utils.ConfigCenter;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import top.trumeet.common.Constants;
import top.trumeet.common.cache.IconCache;
import top.trumeet.common.utils.CustomConfiguration;
import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.db.RegisteredApplicationDb;
import top.trumeet.mipush.provider.register.RegisteredApplication;

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

    private static final int NOTIFICATION_ACTION_BUTTON_PLACE_LEFT = 1;
    private static final int NOTIFICATION_ACTION_BUTTON_PLACE_MID = 2;
    private static final int NOTIFICATION_ACTION_BUTTON_PLACE_RIGHT = 3;
    private static final String NOTIFICATION_STYLE_BIG_PICTURE = "2";
    private static final String NOTIFICATION_STYLE_BIG_PICTURE_URI = "notification_bigPic_uri";
    private static final String NOTIFICATION_STYLE_BIG_TEXT = "1";
    private static final String NOTIFICATION_STYLE_BUTTON_LEFT_INTENT_CLASS = "notification_style_button_left_intent_class";
    private static final String NOTIFICATION_STYLE_BUTTON_LEFT_INTENT_URI = "notification_style_button_left_intent_uri";
    private static final String NOTIFICATION_STYLE_BUTTON_LEFT_NAME = "notification_style_button_left_name";
    private static final String NOTIFICATION_STYLE_BUTTON_LEFT_NOTIFY_EFFECT = "notification_style_button_left_notify_effect";
    private static final String NOTIFICATION_STYLE_BUTTON_LEFT_WEB_URI = "notification_style_button_left_web_uri";
    private static final String NOTIFICATION_STYLE_BUTTON_MID_INTENT_CLASS = "notification_style_button_mid_intent_class";
    private static final String NOTIFICATION_STYLE_BUTTON_MID_INTENT_URI = "notification_style_button_mid_intent_uri";
    private static final String NOTIFICATION_STYLE_BUTTON_MID_NAME = "notification_style_button_mid_name";
    private static final String NOTIFICATION_STYLE_BUTTON_MID_NOTIFY_EFFECT = "notification_style_button_mid_notify_effect";
    private static final String NOTIFICATION_STYLE_BUTTON_MID_WEB_URI = "notification_style_button_mid_web_uri";
    private static final String NOTIFICATION_STYLE_BUTTON_RIGHT_INTENT_CLASS = "notification_style_button_right_intent_class";
    private static final String NOTIFICATION_STYLE_BUTTON_RIGHT_INTENT_URI = "notification_style_button_right_intent_uri";
    private static final String NOTIFICATION_STYLE_BUTTON_RIGHT_NAME = "notification_style_button_right_name";
    private static final String NOTIFICATION_STYLE_BUTTON_RIGHT_NOTIFY_EFFECT = "notification_style_button_right_notify_effect";
    private static final String NOTIFICATION_STYLE_BUTTON_RIGHT_WEB_URI = "notification_style_button_right_web_uri";
    private static final String NOTIFICATION_STYLE_TYPE = "notification_style_type";


    private static boolean tryLoadConfigurations = false;

    private static ExecutorService executorService = Executors.newFixedThreadPool(3);

    /**
     * @see MIPushNotificationHelper#notifyPushMessage
     */
    public static void notifyPushMessage(Context context, byte[] decryptedContent) {
        XmPushActionContainer container = buildContainer(decryptedContent);
        AppInfoUtils.AppNotificationOp notificationOp = AppInfoUtils.getAppNotificationOp(context, getTargetPackage(container));
        if (notificationOp == AppInfoUtils.AppNotificationOp.NOT_ALLOWED) {
            logger.w("Do not notify because user block " + getTargetPackage(container) + "'s notification");
        } else if (TypedShieldHelper.isShield(context, container)) {
            String shieldTypeName = TypedShieldHelper.getShieldType(container);
            logger.w("Do not notify because user block " + shieldTypeName + "'s notification");
        } else {

            String packageName = container.getPackageName();

            if (!tryLoadConfigurations) {
                tryLoadConfigurations = true;
                boolean success = false;
                try {
                    success = Configurations.getInstance().init(context,
                            ConfigCenter.getInstance().getConfigurationDirectory(context)) &&
                            IconConfigurations.getInstance().init(context,
                                    ConfigCenter.getInstance().getConfigurationDirectory(context));
                } catch (Exception e) {
                    Utils.makeText(context, e.toString(), Toast.LENGTH_LONG);
                }
            }

            try {
                Set<String> operations = Configurations.getInstance().handle(packageName, container);

                if (operations.contains(PackageConfig.OPERATION_WAKE)) {
                    wakeScreen(context, packageName);
                }
                if (!operations.contains(PackageConfig.OPERATION_IGNORE)) {
                    executorService.execute(() -> {
                        try {
                            doNotifyPushMessage(context, container, decryptedContent);
                        } catch (Exception e) {
                            logger.e(e.getLocalizedMessage(), e);
                        }
                    });
                }
                if (operations.contains(PackageConfig.OPERATION_OPEN)) {
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

    private static Notification findActiveNotification(String packageName, int notificationId) {
        StatusBarNotification[] notifications = getNotificationManagerEx().getActiveNotifications(packageName);
        for (StatusBarNotification notification : notifications) {
            if (notification.getId() == notificationId) {
                return notification.getNotification();
            }
        }
        return null;
    }

    private static NotificationCompat.Builder addMessage(
            Context context, String packageName, int notificationId,
            NotificationCompat.MessagingStyle.Message message) {
        try {
            Notification activeNotification = findActiveNotification(packageName, notificationId);
            if (activeNotification == null) {
                return null;
            }
            NotificationCompat.MessagingStyle activeStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(activeNotification);
            if (activeStyle == null) {
                return null;
            }
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, activeNotification);
            activeStyle.addMessage(message);
            builder.setStyle(activeStyle);
            return builder;
        } catch (Exception e) {
            logger.e(e.getLocalizedMessage(), e);
            return null;
        }
    }

    private static void doNotifyPushMessage(Context context, XmPushActionContainer container, byte[] decryptedContent) {
        PushMetaInfo metaInfo = container.getMetaInfo();
        String packageName = container.getPackageName();

        String title = metaInfo.getTitle();
        String description = metaInfo.getDescription();

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);

        logger.i("title:" + title + "  description:" + description);


        RegisteredApplication application = RegisteredApplicationDb.registerApplication(
                packageName, false);
        boolean isGroupOfSession = application.getGroupNotificationsForSameSession();

        Context pkgCtx = context;
        if (NotificationManagerEx.INSTANCE.isSystemHookReady()) {
            try {
                pkgCtx = context.createPackageContext(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        NotificationCompat.MessagingStyle.Message message = getMessage(context, container, pkgCtx);
        CustomConfiguration custom = new CustomConfiguration(metaInfo.getExtra());
        boolean useMessagingStyle = message != null && custom.useMessagingStyle(false);

        int notificationId = getNotificationId(container);
        if (isGroupOfSession && !useMessagingStyle) {
            notificationId = (notificationId + "_" + System.currentTimeMillis()).hashCode();
        }

        if (useMessagingStyle) {
            NotificationCompat.Builder messagingBuilder = addMessage(
                    context, packageName, notificationId, message);
            if (messagingBuilder != null) {
                notificationBuilder = messagingBuilder;
            } else {
                Person group = getGroup(context, metaInfo).build();
                NotificationCompat.MessagingStyle style =
                        new NotificationCompat.MessagingStyle(group);
                style.setConversationTitle(group.getName());
                style.setGroupConversation(isGroupConversation(metaInfo));
                style.addMessage(message);
                notificationBuilder.setStyle(style);

                // if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                try {
                    String key = group.getKey() != null ? group.getKey() : group.getName().toString();
                    Intent intent = getSdkIntent(context, container);
                    if (intent == null) {
                        PackageManager packageManager = context.getPackageManager();
                        intent = packageManager.getLaunchIntentForPackage(packageName);
                    }
                    ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(pkgCtx, key)
                            .setIntent(intent)
                            .setLongLived(true)
                            .setShortLabel(group.getName())
                            .setIcon(group.getIcon())
                            .build();

                    ShortcutManagerCompat.pushDynamicShortcut(pkgCtx, shortcut);
                    notificationBuilder.setShortcutInfo(shortcut);
                } catch (Throwable ignore) {
                }
            }
        } else {
            String bigPicUri = getExtraField(metaInfo.getExtra(), "notification_bigPic_uri", null);
            Bitmap bigPic = IconCache.getInstance().getBitmap(context, bigPicUri,
                    (context1, iconUri) -> getBitmapFromUri(
                            context1, iconUri, 1 * MiB));
            if (bigPic != null) {
                NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle();
                style.bigPicture(bigPic);
                style.setBigContentTitle(title);
                notificationBuilder.setStyle(style);
            } else if (description.length() > NOTIFICATION_BIG_STYLE_MIN_LEN) {
                NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
                style.bigText(description);
                style.setBigContentTitle(title);
                notificationBuilder.setStyle(style);
            }

            String[] titleAndDesp = determineTitleAndDespByDIP(context, metaInfo);
            notificationBuilder.setContentTitle(titleAndDesp[0]);
            notificationBuilder.setContentText(titleAndDesp[1]);
        }

        if (metaInfo.getExtra() != null) {
            setNotificationStyleAction(notificationBuilder, context, packageName, metaInfo.getExtra());
        }
        addDebugAction(context, container, decryptedContent, metaInfo, packageName, notificationBuilder);

        notificationBuilder.setWhen(metaInfo.getMessageTs());
        notificationBuilder.setShowWhen(true);

        String group = getGroupName(context, container);
        notificationBuilder.setGroup(group);

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

    @Nullable
    private static NotificationCompat.MessagingStyle.Message getMessage(Context context, XmPushActionContainer container, Context pkgCtx) {
        PushMetaInfo metaInfo = container.metaInfo;
        CustomConfiguration custom = new CustomConfiguration(metaInfo.getExtra());
        String senderMessage = custom.conversationMessage(null);
        if (senderMessage == null) {
            return null;
        }
        boolean atLeastP = pkgCtx != null &&
                pkgCtx.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.P;

        Person person = null;
        if (isGroupConversation(metaInfo) || atLeastP) {
            person = getPerson(context, metaInfo).build();
        }
        return new NotificationCompat.MessagingStyle.Message(
                senderMessage, metaInfo.getMessageTs(), person);
    }

    private static boolean isGroupConversation(PushMetaInfo metaInfo) {
        CustomConfiguration custom = new CustomConfiguration(metaInfo.getExtra());
        return custom.conversationTitle(null) != null;
    }

    @NonNull
    private static Person.Builder getGroup(Context context, PushMetaInfo metaInfo) {
        CustomConfiguration custom = new CustomConfiguration(metaInfo.getExtra());
        String conversation = custom.conversationTitle(null);
        String conversationId = custom.conversationId(null);
        String conversationIcon = custom.conversationIcon(null);

        Person.Builder personBuilder = isGroupConversation(metaInfo) ?
                new Person.Builder() :
                getPerson(context, metaInfo);
        if (conversation != null) {
            personBuilder.setName(conversation);
        } else if (personBuilder.build().getName() == null) {
            personBuilder.setName(metaInfo.getTitle());
        }
        if (conversationId != null) {
            personBuilder.setKey(conversationId);
        }
        Bitmap largeIcon = getLargeIcon(context, metaInfo, conversationIcon);
        if (largeIcon != null) {
            personBuilder.setIcon(IconCompat.createWithBitmap(largeIcon));
        }
        return personBuilder;
    }

    @NonNull
    private static Person.Builder getPerson(Context context, PushMetaInfo metaInfo) {
        CustomConfiguration custom = new CustomConfiguration(metaInfo.getExtra());
        String sender = custom.conversationSender(null);
        String senderId = custom.conversationSenderId(null);
        String senderIcon = custom.conversationSenderIcon(null);

        Person.Builder personBuilder = new Person.Builder().setName(sender);
        personBuilder.setImportant(custom.conversationImportant(false));
        if (senderId != null) {
            personBuilder.setKey(senderId);
        }
        Bitmap largeIcon = getLargeIcon(context, metaInfo, senderIcon);
        if (largeIcon != null) {
            personBuilder.setIcon(IconCompat.createWithBitmap(largeIcon));
        }
        return personBuilder;
    }

    private static void carryPendingIntentForTemporarilyWhitelisted(Context xmPushService, XmPushActionContainer buildContainer, NotificationCompat.Builder localBuilder) {
        PushMetaInfo metaInfo = buildContainer.getMetaInfo();
        // Also carry along the target PendingIntent, whose target will get temporarily whitelisted for background-activity-start upon sent.
        final Intent targetIntent = buildTargetIntentWithoutExtras(buildContainer.getPackageName(), metaInfo);
        final PendingIntent pi = PendingIntent.getService(xmPushService, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        localBuilder.getExtras().putParcelable("mipush.target", pi);
    }

    public static int getNotificationId(XmPushActionContainer container) {
        final PushMetaInfo metaInfo = container.getMetaInfo();
        String id = metaInfo.isSetNotifyId() ? String.valueOf(metaInfo.getNotifyId()) : metaInfo.getId();
        String idWithPackage = MIPushNotificationHelper.getTargetPackage(container) + "_" + id;
        return idWithPackage.hashCode();
    }

    public static String getNotificationTag(String packageName) {
        return "mipush_" + packageName;
    }

    public static String getNotificationTag(XmPushActionContainer container) {
        return getNotificationTag(container.packageName);
    }

    private static String getGroupName(Context xmPushService, XmPushActionContainer buildContainer) {
        PushMetaInfo metaInfo = buildContainer.getMetaInfo();
        String packageName = buildContainer.getPackageName();
        RegisteredApplication application = RegisteredApplicationDb.registerApplication(
                packageName, false);

        boolean groupSession = application != null && application.getGroupNotificationsForSameSession();
        String group = getExtraField(metaInfo.getExtra(), "notification_group", null);
        if (group != null) {
            group = packageName + "_" + GROUP_TYPE_MIPUSH_GROUP + "_" + group;
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
        if (ConfigCenter.getInstance().isDebugMode(xmPushService)) {
            int i = R.drawable.ic_notifications_black_24dp;

            PendingIntent pendingIntentOpenActivity = openActivityPendingIntent(xmPushService, buildContainer, metaInfo, var1);
            if (pendingIntentOpenActivity != null) {
                localBuilder.addAction(new NotificationCompat.Action(i, "Open App", pendingIntentOpenActivity));
            }

            PendingIntent pendingIntentJump = startServicePendingIntent(xmPushService, buildContainer, metaInfo, var1);
            if (pendingIntentJump != null) {
                localBuilder.addAction(new NotificationCompat.Action(i, "Jump", pendingIntentJump));
            }

            Intent sdkIntentJump = getSdkIntent(xmPushService, buildContainer);
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

        boolean useActivity = Boolean.parseBoolean(getExtraField(
                container.metaInfo.getExtra(), "use_clicked_activity", null));
        Intent activityIntent = getSdkIntent(context, container);
        if (!useActivity || activityIntent == null) {
            return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activityIntent.putExtra("mipush_serviceIntent", intent);
        activityIntent.putExtras(intent);
        return PendingIntent.getActivity(context, notificationId, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * @see com.xiaomi.mipush.sdk.PushMessageProcessor#getNotificationMessageIntent
     */
    public static Intent getSdkIntent(Context context, XmPushActionContainer container) {
        String pkgName = container.packageName;
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

    // from sdk 3.7.2
    @TargetApi(16)
    private static NotificationCompat.Builder setNotificationStyleAction(NotificationCompat.Builder builder, Context context, String pkgName, Map<String, String> metaExtra) {
        PendingIntent stylePendingIntent = getStylePendingIntent(context, pkgName, NOTIFICATION_ACTION_BUTTON_PLACE_LEFT, metaExtra);
        if (stylePendingIntent != null && !TextUtils.isEmpty(metaExtra.get(NOTIFICATION_STYLE_BUTTON_LEFT_NAME))) {
            builder.addAction(0, metaExtra.get(NOTIFICATION_STYLE_BUTTON_LEFT_NAME), stylePendingIntent);
        }
        PendingIntent stylePendingIntent2 = getStylePendingIntent(context, pkgName, NOTIFICATION_ACTION_BUTTON_PLACE_MID, metaExtra);
        if (stylePendingIntent2 != null && !TextUtils.isEmpty(metaExtra.get(NOTIFICATION_STYLE_BUTTON_MID_NAME))) {
            builder.addAction(0, metaExtra.get(NOTIFICATION_STYLE_BUTTON_MID_NAME), stylePendingIntent2);
        }
        PendingIntent stylePendingIntent3 = getStylePendingIntent(context, pkgName, NOTIFICATION_ACTION_BUTTON_PLACE_RIGHT, metaExtra);
        if (stylePendingIntent3 != null && !TextUtils.isEmpty(metaExtra.get(NOTIFICATION_STYLE_BUTTON_RIGHT_NAME))) {
            builder.addAction(0, metaExtra.get(NOTIFICATION_STYLE_BUTTON_RIGHT_NAME), stylePendingIntent3);
        }
        return builder;
    }

    private static PendingIntent getStylePendingIntent(Context context, String pkgName, int place, Map<String, String> metaExtra) {
        Intent intent;
        if (metaExtra == null || (intent = getPendingIntentFromExtra(context, pkgName, place, metaExtra)) == null) {
            return null;
        }
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    private static Intent getPendingIntentFromExtra(Context context, String pkgName, int place, Map<String, String> extra) {
        String str;
        String webUriKey;
        String intentUriKey;
        String intentClassKey;
        if (place < NOTIFICATION_ACTION_BUTTON_PLACE_MID) {
            str = NOTIFICATION_STYLE_BUTTON_LEFT_NOTIFY_EFFECT;
        } else {
            str = place < NOTIFICATION_ACTION_BUTTON_PLACE_RIGHT ? NOTIFICATION_STYLE_BUTTON_MID_NOTIFY_EFFECT : NOTIFICATION_STYLE_BUTTON_RIGHT_NOTIFY_EFFECT;
        }
        String typeId = extra.get(str);
        if (TextUtils.isEmpty(typeId)) {
            return null;
        }
        Intent intent = null;
        if (PushConstants.NOTIFICATION_CLICK_DEFAULT.equals(typeId)) {
            try {
                intent = context.getPackageManager().getLaunchIntentForPackage(pkgName);
            } catch (Exception e) {
                logger.e("Cause: " + e.getMessage());
            }
        } else if (PushConstants.NOTIFICATION_CLICK_INTENT.equals(typeId)) {
            if (place < NOTIFICATION_ACTION_BUTTON_PLACE_MID) {
                intentUriKey = NOTIFICATION_STYLE_BUTTON_LEFT_INTENT_URI;
            } else {
                intentUriKey = place < NOTIFICATION_ACTION_BUTTON_PLACE_RIGHT ? NOTIFICATION_STYLE_BUTTON_MID_INTENT_URI : NOTIFICATION_STYLE_BUTTON_RIGHT_INTENT_URI;
            }
            if (place < NOTIFICATION_ACTION_BUTTON_PLACE_MID) {
                intentClassKey = NOTIFICATION_STYLE_BUTTON_LEFT_INTENT_CLASS;
            } else {
                intentClassKey = place < NOTIFICATION_ACTION_BUTTON_PLACE_RIGHT ? NOTIFICATION_STYLE_BUTTON_MID_INTENT_CLASS : NOTIFICATION_STYLE_BUTTON_RIGHT_INTENT_CLASS;
            }
            if (extra.containsKey(intentUriKey)) {
                String intentStr = extra.get(intentUriKey);
                if (intentStr != null) {
                    try {
                        intent = Intent.parseUri(intentStr, Intent.URI_INTENT_SCHEME);
                        intent.setPackage(pkgName);
                    } catch (URISyntaxException e2) {
                        logger.e("Cause: " + e2.getMessage());
                    }
                }
            } else if (extra.containsKey(intentClassKey)) {
                String className = extra.get(intentClassKey);
                intent = new Intent();
                intent.setComponent(new ComponentName(pkgName, className));
            }
        } else if (PushConstants.NOTIFICATION_CLICK_WEB_PAGE.equals(typeId)) {
            if (place < NOTIFICATION_ACTION_BUTTON_PLACE_MID) {
                webUriKey = NOTIFICATION_STYLE_BUTTON_LEFT_WEB_URI;
            } else {
                webUriKey = place < NOTIFICATION_ACTION_BUTTON_PLACE_RIGHT ? NOTIFICATION_STYLE_BUTTON_MID_WEB_URI : NOTIFICATION_STYLE_BUTTON_RIGHT_WEB_URI;
            }
            String uri = extra.get(webUriKey);
            if (!TextUtils.isEmpty(uri)) {
                String tmp = uri.trim();
                if (!tmp.startsWith("http://") && !tmp.startsWith("https://")) {
                    tmp = "http://" + tmp;
                }
                try {
                    URL url = new URL(tmp);
                    String protocol = url.getProtocol();
                    if ("http".equals(protocol) || "https".equals(protocol)) {
                        intent = new Intent("android.intent.action.VIEW");
                        intent.setData(Uri.parse(tmp));
                    }
                } catch (MalformedURLException e3) {
                    logger.e("Cause: " + e3.getMessage());
                }
            }
        }
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                ResolveInfo rinfo = context.getPackageManager().resolveActivity(intent, Intent.FLAG_ACTIVITY_NO_ANIMATION);
                if (rinfo != null) {
                    return intent;
                }
            } catch (Exception e4) {
                logger.e("Cause: " + e4.getMessage());
            }
        }
        return null;
    }


}
