package com.nihility;

import com.nihility.service.XMPushServiceListener;
import com.xiaomi.push.service.XMPushService;

import java.lang.reflect.Field;

public class Dependencies {
    private Configurations configurations;

    public interface XMPushServiceListenerGetter {
        XMPushServiceListener create(XMPushService pushService);
    }
    private XMPushServiceListenerGetter serviceListenerGetter;

    public void init(Configurations configurations) {
        this.configurations = configurations;
    }

    public void init(XMPushServiceListenerGetter serviceListenerGetter) {
        this.serviceListenerGetter = serviceListenerGetter;
    }

    public void check() throws NullPointerException {
        Field[] fields = Dependencies.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                Object value = field.get(this);
                if (value == null) {
                    throw new NullPointerException("Dependencies not initialized: " + field.getName());
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public Configurations configuration() {
        return configurations;
    }

    public XMPushServiceListener serviceListener(XMPushService pushService) {
        return serviceListenerGetter.create(pushService);
    }

    private static class LazyHolder {
        static Dependencies INSTANCE = new Dependencies();
    }

    public static Dependencies getInstance() {
        return LazyHolder.INSTANCE;
    }
}

