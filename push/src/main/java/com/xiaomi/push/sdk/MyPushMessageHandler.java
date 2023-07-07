package com.xiaomi.push.sdk;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.P;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.catchingnow.icebox.sdk_client.IceBox;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.topjohnwu.superuser.Shell;
import com.xiaomi.push.service.MIPushEventProcessor;
import com.xiaomi.push.service.MIPushNotificationHelper;
import com.xiaomi.push.service.MyMIPushNotificationHelper;
import com.xiaomi.push.service.PushConstants;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmsf.push.notification.NotificationController;
import com.xiaomi.xmsf.push.utils.Configurations;
import com.xiaomi.xmsf.utils.ConfigCenter;

import org.json.JSONException;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.function.Consumer;

import top.trumeet.common.Constants;
import top.trumeet.common.ita.ITopActivity;
import top.trumeet.common.ita.TopActivityFactory;
import top.trumeet.common.utils.CustomConfiguration;
import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.db.RegisteredApplicationDb;
import top.trumeet.mipush.provider.register.RegisteredApplication;

/**
 * @author zts1993
 * @date 2018/2/9
 */

public class MyPushMessageHandler extends IntentService {
    private static Logger logger = XLog.tag("MyPushMessageHandler").build();

    private static final int APP_CHECK_FRONT_MAX_RETRY = 8;
    private static final int APP_CHECK_SLEEP_DURATION_MS = 500;
    private static final int APP_CHECK_SLEEP_MAX_TIMEOUT_MS = APP_CHECK_FRONT_MAX_RETRY * APP_CHECK_SLEEP_DURATION_MS;

    static ITopActivity iTopActivity = null;

    public MyPushMessageHandler() {
        super("my mipush message handler");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        byte[] payload = intent.getByteArrayExtra(PushConstants.MIPUSH_EXTRA_PAYLOAD);
        if (payload == null) {
            logger.e("mipush_payload is null");
            return;
        }

        final XmPushActionContainer container = MIPushEventProcessor.buildContainer(payload);
        if (container == null) {
            return;
        }

        try {
            if (startService(this, container, payload) != null) {
                cancelNotification(this, intent.getExtras(), container);
            }
        } catch (Exception e) {
            logger.e(e.getLocalizedMessage(), e);
        }

    }

    public static void cancelNotification(Context context, Bundle bundle) {
        byte[] payload = bundle.getByteArray(PushConstants.MIPUSH_EXTRA_PAYLOAD);
        if (payload == null) {
            logger.e("mipush_payload is null");
            return;
        }

        final XmPushActionContainer container = MIPushEventProcessor.buildContainer(payload);
        if (container == null) {
            return;
        }
        cancelNotification(context, bundle, container);
    }

    public static void cancelNotification(Context context, Bundle bundle, XmPushActionContainer container) {
        int notificationId = bundle.getInt(Constants.INTENT_NOTIFICATION_ID, 0);
        String notificationGroup = bundle.getString(Constants.INTENT_NOTIFICATION_GROUP);
        try {
            Configurations.getInstance().handle(container.packageName, container);
        } catch (Exception e) {
            logger.e("cancelNotification", e);
        }
        CustomConfiguration custom = new CustomConfiguration(container.metaInfo != null ?
                container.metaInfo.extra : new HashMap<>());

        NotificationController.cancel(context, container,
                notificationId, notificationGroup, custom.clearGroup(false));
    }

    public static void launchApp(Context context, XmPushActionContainer container) {
        if (iTopActivity == null) {
            iTopActivity = TopActivityFactory.newInstance(ConfigCenter.getInstance().getAccessMode(context));
        }

        if (!iTopActivity.isEnabled(context)) {
            iTopActivity.guideToEnable(context);
            return;
        }

        String targetPackage = container.getPackageName();

        activeApp(context, targetPackage);
        pullUpApp(context, targetPackage, container);
    }

    public static ComponentName startService(Context context, XmPushActionContainer container, byte[] payload) {
        launchApp(context, container);

        return forwardToTargetApplication(context, payload);
    }

    public static ComponentName forwardToTargetApplication(Context context, byte[] payload) {
        XmPushActionContainer container = MIPushEventProcessor.buildContainer(payload);
        PushMetaInfo metaInfo = container.getMetaInfo();
        String targetPackage = container.getPackageName();

        final Intent localIntent = new Intent(PushConstants.MIPUSH_ACTION_NEW_MESSAGE);
        localIntent.setComponent(new ComponentName(targetPackage, "com.xiaomi.mipush.sdk.PushMessageHandler"));
        localIntent.putExtra(PushConstants.MIPUSH_EXTRA_PAYLOAD, payload);
        localIntent.putExtra(MIPushNotificationHelper.FROM_NOTIFICATION, true);
        localIntent.addCategory(String.valueOf(metaInfo.getNotifyId()));
        logger.d(packageInfo(targetPackage, "send to service"));
        ComponentName componentName = context.startService(localIntent);
        return componentName;
    }

    private static void activeApp(Context context, String targetPackage) {
        if (ConfigCenter.getInstance().isIceboxSupported(context) &&
                Utils.isAppInstalled(IceBox.PACKAGE_NAME)) {
            try {
                if (ContextCompat.checkSelfPermission(context, IceBox.SDK_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
                    int enabledSetting = IceBox.getAppEnabledSetting(context, targetPackage);
                    if (enabledSetting != 0) {
                        logger.w(packageInfo(targetPackage, "active app by IceBox SDK"));
                        IceBox.setAppEnabledSettings(context, true, targetPackage);
                        return;
                    }
                } else {
                    logger.w(packageInfo(targetPackage, "skip active app by IceBox SDK due to lack of permissions"));
                }
            } catch (Throwable e) {
                logger.e(packageInfo(targetPackage, "activeApp failed " + e.getLocalizedMessage()), e);
            }
        }
        Shell.cmd("pm enable " + targetPackage).exec();
    }


    private static Intent getJumpIntent(Context context, XmPushActionContainer container) {
        Intent intent = MyMIPushNotificationHelper.getSdkIntent(context, container);
        if (intent == null) {
            intent = getJumpIntentFromPkg(context, container.packageName);
        }
        return intent;
    }

    private static Intent getJumpIntentFromPkg(Context context, String targetPackage) {
        Intent intent = null;
        try {
            intent = context.getPackageManager().getLaunchIntentForPackage(targetPackage);
            logger.d(packageInfo(targetPackage, intent.toString()));
        } catch (RuntimeException ignore) {
        }
        return intent;
    }

    private void runWithAppStateElevatedToForeground(final String pkg, final Consumer<Boolean> task) {
        if (SDK_INT < P) {      // onNullBinding() was introduced in Android P.
            task.accept(Boolean.FALSE);
            return;
        }
        final Intent intent = new Intent().setClassName(pkg, MyMIPushNotificationHelper.CLASS_NAME_PUSH_MESSAGE_HANDLER);
        final Context appContext = getApplicationContext();
        final boolean successful = appContext.bindService(intent, new ServiceConnection() {

            private void runTaskAndUnbind() {
                task.accept(Boolean.TRUE);
                appContext.unbindService(this);
            }

            @RequiresApi(P) @Override public void onNullBinding(final ComponentName name) {
                runTaskAndUnbind();
            }

            @Override public void onServiceConnected(final ComponentName name, final IBinder service) {
                runTaskAndUnbind();     // Should not happen
            }

            @Override public void onServiceDisconnected(final ComponentName name) {}
        }, BIND_AUTO_CREATE | BIND_IMPORTANT | BIND_ABOVE_CLIENT);

        if (! successful) task.accept(Boolean.FALSE);
    }

    private static long pullUpApp(Context context, String targetPackage, XmPushActionContainer container) {
        long start = System.currentTimeMillis();

        try {


            if (!iTopActivity.isAppForeground(context, targetPackage)) {
                logger.d(packageInfo(targetPackage, "app is not at front , let's pull up"));

                Intent intent = getJumpIntent(context, container);

                if (intent == null) {
                    throw new RuntimeException("can not get default activity for " + targetPackage);
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                    context.startActivity(intent);
                    logger.d(packageInfo(targetPackage, "start activity"));
                }


                //wait
                for (int i = 0; i < APP_CHECK_FRONT_MAX_RETRY; i++) {

                    if (!iTopActivity.isAppForeground(context, targetPackage)) {
                        Thread.sleep(APP_CHECK_SLEEP_DURATION_MS);
                    } else {
                        break;
                    }

                    if (i == (APP_CHECK_FRONT_MAX_RETRY / 2)) {
                        intent = getJumpIntentFromPkg(context, targetPackage);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        context.startActivity(intent);
                    }
                }

                if ((System.currentTimeMillis() - start) >= APP_CHECK_SLEEP_MAX_TIMEOUT_MS) {
                    logger.w(packageInfo(targetPackage, "pull up app timeout"));
                }

            } else {
                logger.d(packageInfo(targetPackage, "app is at foreground"));
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            logger.e(packageInfo(targetPackage, "pullUpApp failed " + e.getLocalizedMessage()), e);
        }


        long end = System.currentTimeMillis();
        return end - start;

    }

    private static String packageInfo(String packageName, String message) {
        return "[" + packageName +"] " + message;
    }
}

