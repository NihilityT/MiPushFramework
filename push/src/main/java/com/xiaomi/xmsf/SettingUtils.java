package com.xiaomi.xmsf;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.settings.widget.RegistrationHelper;
import com.catchingnow.icebox.sdk_client.IceBox;
import com.nihility.InternalMessenger;
import com.xiaomi.push.service.PushConstants;
import com.xiaomi.push.service.PushServiceConstants;
import com.xiaomi.push.service.XMPushServiceMessenger;
import com.xiaomi.smack.ConnectionConfiguration;
import com.xiaomi.xmsf.push.notification.NotificationController;
import com.xiaomi.xmsf.utils.ConfigCenter;
import com.xiaomi.xmsf.utils.LogUtils;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import top.trumeet.common.Constants;
import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.db.EventDb;
import top.trumeet.mipush.provider.register.RegisteredApplication;
import top.trumeet.mipushframework.main.ApplicationPageOperation;

public class SettingUtils {
    public static final int requestIceBoxCode = 0x233;
    public static AtomicBoolean mClearingHistory = new AtomicBoolean(false);

    public static void requestIceBoxPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{IceBox.SDK_PERMISSION}, requestIceBoxCode);
    }

    public static boolean iceBoxPermissionGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, IceBox.SDK_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void clearLog(Context context) {
        Toast.makeText(context, context.getString(R.string.settings_clear_log) + " " + context.getString(R.string.start), Toast.LENGTH_SHORT).show();
        LogUtils.clearLog(context);
        Toast.makeText(context, context.getString(R.string.settings_clear_log) + " " + context.getString(R.string.end), Toast.LENGTH_SHORT).show();
    }

    public static void clearHistory(Context context) {
        if (mClearingHistory.compareAndSet(false, true)) {
            new Thread(() -> {
                Utils.makeText(context, context.getString(R.string.settings_clear_history) + " " + context.getString(R.string.start), Toast.LENGTH_SHORT);
                EventDb.deleteHistory();
                Utils.makeText(context, context.getString(R.string.settings_clear_history) + " " + context.getString(R.string.end), Toast.LENGTH_SHORT);
                mClearingHistory.set(false);
            }).start();
        }
    }

    public static void startMiPushServiceAsForegroundService(Context context) {
        new InternalMessenger(context).send(new Intent(XMPushServiceMessenger.IntentStartForeground));
    }

    public static void notifyMockNotification(Context context) {
        String packageName = BuildConfig.APPLICATION_ID;
        Date date = new Date();
        String title = context.getString(R.string.debug_test_title);
        String description = context.getString(R.string.debug_test_content) + date.toString();
        NotificationController.test(context, packageName, title, description);
    }

    public static boolean isIceBoxInstalled() {
        return Utils.isAppInstalled(IceBox.PACKAGE_NAME);
    }

    public static void tryForceRegisterAllApplications() {
        ApplicationPageOperation.MiPushApplications miPushApplications =
                ApplicationPageOperation.getMiPushApplications();
        for (RegisteredApplication registeredApplication : miPushApplications.res) {
            RegistrationHelper.tryForceRegister(registeredApplication.getPackageName());
        }
    }

    public static void sendXMPPReconnectRequest(Context context) {
        new InternalMessenger(context).send(
                new Intent(PushConstants.ACTION_RESET_CONNECTION));
    }

    public static void setXMPPServer(Context context, String newHost) {
        ConfigCenter.getInstance().setXMPPServer(context, newHost);
    }

    public static @NonNull String getXMPPServerHint() {
        return ConnectionConfiguration.getXmppServerHost() + ":" + PushServiceConstants.XMPP_SERVER_PORT;
    }

    public static String getXMPPServer(Context context) {
        return ConfigCenter.getInstance().getXMPPServer(context);
    }

    public static Uri getConfigurationDirectory(Context context) {
        return ConfigCenter.getInstance().getConfigurationDirectory(context);
    }

    public static void shareLogs(Context context) {
        context.startActivity(new Intent()
                .setComponent(new ComponentName(Constants.SERVICE_APP_NAME,
                        Constants.SHARE_LOG_COMPONENT_NAME)));
    }

    public static @NonNull Uri saveConfigurationUri(Context context, Intent data) {
        Uri uri = data.getData();
        setConfigurationDirectory(context, uri);
        return uri;
    }

    public static void setConfigurationDirectory(Context context, Uri uri) {
        context.getContentResolver().takePersistableUriPermission(uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        ConfigCenter.getInstance().setConfigurationDirectory(context, uri);
        ConfigCenter.getInstance().loadConfigurations(context);
    }

}