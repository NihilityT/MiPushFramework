package com.nihility;

import com.nihility.service.RegistrationRecorder;
import com.nihility.utils.Singleton;
import com.xiaomi.xmsf.push.utils.ConfigValueConverter;
import com.xiaomi.xmsf.push.utils.IconConfigurations;
import com.xiaomi.xmsf.utils.ConfigCenter;

import top.trumeet.common.cache.ApplicationNameCache;
import top.trumeet.common.cache.IconCache;

public class Global {
    public static MethodHooker MethodHooker() {
        return Singleton.instance();
    }

    public static void setMethodHooker(MethodHooker methodHooker) {
        Singleton.reset(methodHooker);
    }

    public static MiPushEventListener MiPushEventListener() {
        return Singleton.instance();
    }

    public static void setMiPushEventListener(MiPushEventListener listener) {
        Singleton.reset(listener);
    }

    public static RegistrationRecorder RegistrationRecorder() {
        return Singleton.instance();
    }

    public static void setRegistrationRecorder(RegistrationRecorder instance) {
        Singleton.reset(instance);
    }

    public static ConfigValueConverter ConfigValueConverter() {
        return Singleton.instance();
    }

    public static IconConfigurations IconConfigurations() {
        return Singleton.instance();
    }

    public static ConfigCenter ConfigCenter() {
        return Singleton.instance();
    }

    public static void setConfigCenter(ConfigCenter configCenter) {
        Singleton.reset(configCenter);
    }

    public static ApplicationNameCache ApplicationNameCache() {
        return Singleton.instance();
    }

    public static IconCache IconCache() {
        return Singleton.instance();
    }
}