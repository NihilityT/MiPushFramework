package com.xiaomi.xmsf;

import static com.xiaomi.xmsf.push.control.PushControllerUtils.isAppMainProc;
import static com.xiaomi.xmsf.push.notification.NotificationController.CHANNEL_WARN;
import static top.trumeet.common.Constants.TAG_CONDOM;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationChannelGroupCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.elvishew.xlog.XLog;
import com.nihility.notification.NotificationManagerEx;
import com.nihility.utils.Hooker;
import com.oasisfeng.condom.CondomOptions;
import com.oasisfeng.condom.CondomProcess;
import com.topjohnwu.superuser.Shell;
import com.xiaomi.xmsf.push.control.PushControllerUtils;
import com.xiaomi.xmsf.push.control.XMOutbound;
import com.xiaomi.xmsf.push.service.MiuiPushActivateService;
import com.xiaomi.xmsf.utils.LogUtils;

import top.trumeet.common.Constants;
import top.trumeet.common.push.PushServiceAccessibility;
import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.DatabaseUtils;


public class MiPushFrameworkApp extends Application {
    private com.elvishew.xlog.Logger logger;

    private static final String MIPUSH_EXTRA = "mipush_extra";

    private static MiPushFrameworkApp instance;


    static {
        // Set settings before the main shell can be created
        Shell.enableVerboseLogging = BuildConfig.DEBUG;
        Shell.setDefaultBuilder(Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        );
    }

    public static Context getContext(){
        return instance;
    }

    @Override
    public void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        DatabaseUtils.init(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        tryToGetHighPrivilege();

        initGlobalContext();
        initBasicLogger();

        Hooker.setLogger(PushControllerUtils.wrapContext(this));
        Hooker.hook(this);

        NotificationManagerEx.init(getApplicationContext());


        installCondom();

        PushControllerUtils.setAllEnable(true, this);

        long currentTimeMillis = System.currentTimeMillis();
        long lastStartupTime = getLastStartupTime();
        if (isAppMainProc(this)) {
            if ((currentTimeMillis - lastStartupTime > 300000 || currentTimeMillis - lastStartupTime < 0)) {
                setStartupTime(currentTimeMillis);
                MiuiPushActivateService.awakePushActivateService(PushControllerUtils.wrapContext(this)
                        , "com.xiaomi.xmsf.push.SCAN");
            }
        }

        try {
            if (!PushServiceAccessibility.isInDozeWhiteList(this)) {
                NotificationManagerCompat manager = NotificationManagerCompat.from(this);
                notifyDozeWhiteListRequest(manager);
            }
        } catch (RuntimeException e) {
            logger.e(e.getMessage(), e);
        }


    }

    private void installCondom() {
        CondomOptions options = XMOutbound.create(this, TAG_CONDOM + "_PROCESS",
                false);
        CondomProcess.installExceptDefaultProcess(this, options);
    }

    private static void tryToGetHighPrivilege() {
        Shell.getShell();
    }

    private void initGlobalContext() {
        instance = this;
        Utils.context = this;
    }


    private void initBasicLogger() {
        LogUtils.init(this);
        logger = XLog.tag(MiPushFrameworkApp.class.getSimpleName()).build();
        logger.i("App starts: " + BuildConfig.VERSION_NAME);
    }

    private void notifyDozeWhiteListRequest(NotificationManagerCompat manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannelCompat.Builder channel = new NotificationChannelCompat
                    .Builder(CHANNEL_WARN, NotificationManager.IMPORTANCE_HIGH)
                    .setName(getString(R.string.wizard_title_doze_whitelist));

            NotificationChannelGroupCompat notificationChannelGroup =
                    new NotificationChannelGroupCompat.Builder(CHANNEL_WARN).setName(CHANNEL_WARN).build();
            manager.createNotificationChannelGroup(notificationChannelGroup);
            channel.setGroup(notificationChannelGroup.getId());
            manager.createNotificationChannel(channel.build());
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent()
                .setComponent(new ComponentName(Constants.SERVICE_APP_NAME,
                        Constants.REMOVE_DOZE_COMPONENT_NAME)), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this,
                CHANNEL_WARN)
                .setContentInfo(getString(R.string.wizard_title_doze_whitelist))
                .setContentTitle(getString(R.string.wizard_title_doze_whitelist))
                .setContentText(getString(R.string.wizard_descr_doze_whitelist))
                .setTicker(getString(R.string.wizard_descr_doze_whitelist))
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setShowWhen(true)
                .setAutoCancel(true)
                .build();
        manager.notify(getClass().getSimpleName(), 100, notification);  // Use tag to avoid conflict with push notifications.
    }


    private long getLastStartupTime() {
        return getDefaultPreferences().getLong("xmsf_startup", 0);
    }

    private boolean setStartupTime(long j) {
        return getDefaultPreferences().edit().putLong("xmsf_startup", j).commit();
    }

    private SharedPreferences getDefaultPreferences() {
        return getSharedPreferences(MIPUSH_EXTRA, 0);
    }

}
