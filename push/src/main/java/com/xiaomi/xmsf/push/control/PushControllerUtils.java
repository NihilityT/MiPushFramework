package com.xiaomi.xmsf.push.control;

import static top.trumeet.common.Constants.TAG_CONDOM;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.oasisfeng.condom.CondomContext;
import com.xiaomi.channel.commonutils.logger.MyLog;
import com.xiaomi.channel.commonutils.misc.ScheduledJobManager;
import com.xiaomi.mipush.sdk.MiPushClient;
import com.xiaomi.push.service.PushServiceConstants;
import com.xiaomi.xmsf.FirstRegister;
import com.xiaomi.xmsf.RetryRegister;
import com.xiaomi.xmsf.push.service.receivers.BootReceiver;
import com.xiaomi.xmsf.push.service.receivers.KeepAliveReceiver;

import java.util.Objects;

import top.trumeet.common.Constants;

/**
 * Created by Trumeet on 2017/8/25.
 *
 * @author Trumeet
 */

@SuppressLint("WrongConstant")
public class PushControllerUtils {
    private static Logger logger = XLog.tag(PushControllerUtils.class.getSimpleName()).build();

    private static BroadcastReceiver liveReceiver = new KeepAliveReceiver();

    private static final int[] RetryInterval = {3600000, 7200000, 14400000, 28800000, 86400000};

    public static void registerPush(Context context, int i) {
        Objects.requireNonNull(context);
        int[] retryInterval = RetryInterval;
        int length = retryInterval.length;
        long intervalMs = i < length ? retryInterval[i] : retryInterval[length - 1];
        MyLog.i("for make sure xmsf register push succ, schedule register after " + intervalMs / 1000 + " sec");
        new Handler(Looper.getMainLooper()).postDelayed(new RetryRegister(context, i), intervalMs);
    }

    public static boolean pushRegistered(final Context context) {
        return !TextUtils.isEmpty(MiPushClient.getRegId(context));
    }

    private static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    /**
     * Get is user enable push in settings.
     *
     * @param context Context param
     * @return is enable
     * @see Constants#KEY_ENABLE_PUSH
     */
    public static boolean isPrefsEnable(Context context) {
        return getPrefs(context)
                .getBoolean(Constants.KEY_ENABLE_PUSH, true);
    }

    /**
     * Set push enable
     *
     * @param value   is enable
     * @param context Context param
     * @see Constants#KEY_ENABLE_PUSH
     */
    public static void setPrefsEnable(boolean value, Context context) {
        getPrefs(context)
                .edit()
                .putBoolean(Constants.KEY_ENABLE_PUSH, value)
                .apply();
    }

    /**
     * Check is in main app processMIPushMessage
     *
     * @param context Context param
     * @return is in main processMIPushMessage
     */
    public static boolean isAppMainProc(Context context) {
        for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : ((ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE))
                .getRunningAppProcesses()) {
            if (runningAppProcessInfo.pid == Process.myPid() && runningAppProcessInfo.processName.equals(context.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set XMPush sdk enable
     *
     * @param enable  enable
     * @param context context param
     */
    public static void setServiceEnable(boolean enable, Context context) {
        if (enable) {
            logger.d("Starting...");


            if (isAppMainProc(context)) {
                ScheduledJobManager.getInstance(wrapContext(context))
                        .addOneShootJob(new FirstRegister(wrapContext(context)));
            }

            try {
                Intent serviceIntent = new Intent(context, com.xiaomi.push.service.XMPushService.class);
                serviceIntent.putExtra(PushServiceConstants.EXTRA_TIME_STAMP, System.currentTimeMillis());
                serviceIntent.setAction(PushServiceConstants.ACTION_TIMER);
                ContextCompat.startForegroundService(context, serviceIntent);
            } catch (Throwable e) {
                logger.e(e);
            }

            try {
                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_SCREEN_ON);
                context.registerReceiver(liveReceiver, filter);
            } catch (Throwable e) {
                logger.e(e);
            }

        } else {
            logger.d("Stopping...");

            try {
                context.unregisterReceiver(liveReceiver);
            } catch (Throwable e) {
                logger.e(e);
            }

            MiPushClient.unregisterPush(wrapContext(context));
            // Force stop and disable services.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                scheduler.cancelAll();
            }
            context.stopService(new Intent(context, com.xiaomi.push.service.XMPushService.class));
        }
    }

    /**
     * Set SP and XMPush enable
     *
     * @param enable  is enable
     * @param context Context param
     */
    public static void setAllEnable(boolean enable, Context context) {
        setPrefsEnable(enable, context);
        setServiceEnable(enable, context);
        setBootReceiverEnable(enable, context);
    }


    @SuppressLint("WrongConstant")
    private static void setBootReceiverEnable(boolean enable, Context context) {
        context.getPackageManager()
                .setComponentEnabledSetting(new ComponentName(context,
                                BootReceiver.class),
                        enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
    }

    public static Context wrapContext(final Context context) {
        return CondomContext.wrap(context, TAG_CONDOM, XMOutbound.create(context,
                TAG_CONDOM));
    }
}
