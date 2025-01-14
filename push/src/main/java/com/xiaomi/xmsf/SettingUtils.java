package com.xiaomi.xmsf;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.catchingnow.icebox.sdk_client.IceBox;
import com.nihility.InternalMessenger;
import com.xiaomi.push.service.XMPushServiceMessenger;
import com.xiaomi.xmsf.push.notification.NotificationController;
import com.xiaomi.xmsf.utils.LogUtils;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.db.EventDb;

public class SettingUtils {
    static final int requestIceBoxCode = 0x233;
    static AtomicBoolean mClearingHistory = new AtomicBoolean(false);

    static void requestIceBoxPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{IceBox.SDK_PERMISSION}, requestIceBoxCode);
    }

    static boolean iceBoxPermissionGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, IceBox.SDK_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    static void clearLog(Context context) {
        Toast.makeText(context, context.getString(R.string.settings_clear_log) + " " + context.getString(R.string.start), Toast.LENGTH_SHORT).show();
        LogUtils.clearLog(context);
        Toast.makeText(context, context.getString(R.string.settings_clear_log) + " " + context.getString(R.string.end), Toast.LENGTH_SHORT).show();
    }

    static void clearHistory(Context context) {
        if (mClearingHistory.compareAndSet(false, true)) {
            new Thread(() -> {
                Utils.makeText(context, context.getString(R.string.settings_clear_history) + " " + context.getString(R.string.start), Toast.LENGTH_SHORT);
                EventDb.deleteHistory();
                Utils.makeText(context, context.getString(R.string.settings_clear_history) + " " + context.getString(R.string.end), Toast.LENGTH_SHORT);
                mClearingHistory.set(false);
            }).start();
        }
    }

    static void startMiPushServiceAsForegroundService(Context context) {
        new InternalMessenger(context).send(new Intent(XMPushServiceMessenger.IntentStartForeground));
    }

    static void notifyMockNotification(Context context) {
        String packageName = BuildConfig.APPLICATION_ID;
        Date date = new Date();
        String title = context.getString(R.string.debug_test_title);
        String description = context.getString(R.string.debug_test_content) + date.toString();
        NotificationController.test(context, packageName, title, description);
    }

    static boolean isIceBoxInstalled() {
        return Utils.isAppInstalled(IceBox.PACKAGE_NAME);
    }
}