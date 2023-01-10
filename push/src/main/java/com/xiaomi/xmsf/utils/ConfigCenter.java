package com.xiaomi.xmsf.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.xiaomi.xmsf.BuildConfig;
import com.xiaomi.xmsf.push.service.XMPushService;
import com.xiaomi.xmsf.push.utils.Configurations;

import top.trumeet.common.Constants;
import top.trumeet.common.utils.CustomNotifyIcon;


/**
 * Push 配置
 * @author zts
 */
public class ConfigCenter {

    private static class LazyHolder {
        volatile static ConfigCenter INSTANCE = new ConfigCenter();
    }


    public static ConfigCenter getInstance() {
        return LazyHolder.INSTANCE;
    }

    private ConfigCenter() {
    }

    //using MODE_MULTI_PROCESS emmm.....
    private SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_MULTI_PROCESS);
    }

    public boolean isNotificationOnRegister(Context ctx) {
        return getSharedPreferences(ctx).getBoolean("NotificationOnRegister", false);
    }

    public int getAccessMode(Context ctx) {
        String mode = getSharedPreferences(ctx).getString("AccessMode", "0");
        return Integer.valueOf(mode);
    }

    public boolean isIceboxSupported(Context ctx) {
        return getSharedPreferences(ctx).getBoolean("IceboxSupported", false);
    }

    public Uri getConfigurationDirectory(Context ctx) {
        String uri = getSharedPreferences(ctx).getString("ConfigurationDirectory", null);
        return uri == null ? null : Uri.parse(uri);
    }

    public boolean setConfigurationDirectory(Context ctx, Uri treeUri) {
        return getSharedPreferences(ctx).edit()
                .putString("ConfigurationDirectory", treeUri.toString())
                .commit();
    }

    public Uri getCustomNotifyIconPath(Context ctx) {
        String uri = getSharedPreferences(ctx).getString("CustomNotifyIconPath", null);
        return uri == null ? null : Uri.parse(uri);
    }

    public boolean setCustomNotifyIconPath(Context ctx, Uri treeUri) {
        return getSharedPreferences(ctx).edit()
                .putString("CustomNotifyIconPath", treeUri.toString())
                .commit();
    }

    public void loadConfigurations(Context context) {
        CustomNotifyIcon.getInstance().init(context,
                ConfigCenter.getInstance().getCustomNotifyIconPath(context));
        Configurations.getInstance().init(context,
                ConfigCenter.getInstance().getConfigurationDirectory(context));
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context, XMPushService.class));
        intent.setAction(Constants.CONFIGURATIONS_UPDATE_ACTION);
        context.startService(intent);
    }
}
