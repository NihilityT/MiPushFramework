package com.nihility.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.nihility.Configurations;
import com.nihility.Dependencies;
import com.nihility.service.XMPushServiceAbility;
import com.xiaomi.channel.commonutils.android.Region;
import com.xiaomi.network.HostManager;
import com.xiaomi.push.service.AppRegionStorage;
import com.xiaomi.smack.ConnectionConfiguration;
import com.xiaomi.smack.SmackConfiguration;
import com.xiaomi.xmsf.utils.ConfigCenter;

import java.lang.reflect.Field;

public class Hooker {
    private static final Logger logger = XLog.tag("Hooker").build();

    public static void hook(Context context) {
        initMiPushHookLib(context);
        hookMiPushSDK(context);
    }

    private static void initMiPushHookLib(final Context context) {
        Configurations configurations = new Configurations() {
            @NonNull
            @Override
            public String getXMPPServer() {
                return ConfigCenter.getInstance().getXMPPServer(context.getApplicationContext());
            }
        };
        Dependencies dependencies = Dependencies.getInstance();
        dependencies.init(configurations);
        dependencies.init(pushService -> new XMPushServiceAbility(pushService));
        dependencies.check();
    }

    private static void hookMiPushSDK(final Context context) {
        try {
            hookField(SmackConfiguration.class, "pingInterval", 3 * 60 * 1000);
            hookMiPushServerHost();
            AppRegionStorage regionStorage = AppRegionStorage.getInstance(context.getApplicationContext());
            regionStorage.setRegion(Region.China.name());
            regionStorage.setCountryCode("CN");
        } catch (Throwable e) {
           logger.e(e.getMessage(), e);
        }
    }

    private static void hookMiPushServerHost() {
        addReservedHost(ConnectionConfiguration.XMPP_SERVER_CHINA_HOST_P, new String[]{
                ConnectionConfiguration.XMPP_SERVER_CHINA_HOST_P,
                "220.181.106.151:5222",
                "220.181.106.151:443",
                "220.181.106.152:5222",
                "118.26.252.226:443",
                "118.26.252.225:443",
                "58.83.177.235:5222",
                "58.83.177.220:5222",
        });

        String resolver = "resolver.msg.xiaomi.net";
        addReservedHost(resolver, new String[]{
                resolver,
                "111.13.142.153:5222",
                "118.26.252.209:5222",
                "39.156.150.162:5222",
                "111.13.142.153:80",
                "39.156.150.162:80",
                "123.125.102.48:5222",
                "220.181.106.150:5222",
                "118.26.252.209:5222",
        });
    }

    private static void addReservedHost(String host, String[] hosts) {
        for (String h : hosts) {
            HostManager.addReservedHost(host, h);
        }
    }

    private static void hookField(Class klass, String field, Object value) {
        try {
            Field target = klass.getDeclaredField(field);
            target.setAccessible(true);
            target.set(null, value);
        } catch (Throwable e) {
           logger.e(e.getMessage(), e);
        }
    }
}