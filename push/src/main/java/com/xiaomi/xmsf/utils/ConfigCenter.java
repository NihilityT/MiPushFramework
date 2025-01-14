package com.xiaomi.xmsf.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.xiaomi.xmsf.BuildConfig;
import com.xiaomi.xmsf.push.service.XMPushService;
import com.xiaomi.xmsf.push.utils.Configurations;
import com.xiaomi.xmsf.push.utils.IconConfigurations;

import top.trumeet.common.Constants;
import top.trumeet.common.utils.Utils;


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
    public static void setInstance(ConfigCenter configCenter) {
        LazyHolder.INSTANCE = configCenter;
    }

    public ConfigCenter() {
    }

    //using MODE_MULTI_PROCESS emmm.....
    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_MULTI_PROCESS);
    }

    public boolean isNotificationOnRegister(Context ctx) {
        return getSharedPreferences(ctx).getBoolean("NotificationOnRegister", false);
    }

    public boolean isShowConfigurationListOnLoaded(Context ctx) {
        return getSharedPreferences(ctx).getBoolean("ShowConfigurationListOnLoaded", false);
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

    public String getXMPPServer(Context ctx) {
        return getSharedPreferences(ctx).getString("XMPP_server", null);
    }

    public boolean setXMPPServer(Context ctx, String host) {
        return getSharedPreferences(ctx).edit()
                .putString("XMPP_server", host)
                .commit();
    }

    public boolean isDebugMode() {
        return getSharedPreferences(Utils.getApplication()).getBoolean("DebugMode", false);
    }

    public boolean isShowAllEvents() {
        return getSharedPreferences(Utils.getApplication()).getBoolean("ShowAllEvents", false);
    }

    public boolean isStartForegroundService() {
        return getSharedPreferences(Utils.getApplication()).getBoolean("StartForegroundService", false);
    }

    public void loadConfigurations(Context context) {
        Configurations.getInstance().init(context,
                ConfigCenter.getInstance().getConfigurationDirectory(context));
        IconConfigurations.getInstance().init(context,
                ConfigCenter.getInstance().getConfigurationDirectory(context));
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context, XMPushService.class));
        intent.setAction(Constants.CONFIGURATIONS_UPDATE_ACTION);
        context.startService(intent);
    }
}
