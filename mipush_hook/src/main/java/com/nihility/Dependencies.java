package com.nihility;

import com.nihility.service.XMPushServiceListener;

import java.lang.reflect.Field;

public class Dependencies {
    private Configurations configurations;
    private XMPushServiceListener serviceListener;

    public void init(Configurations configurations) {
        this.configurations = configurations;
    }

    public void init(XMPushServiceListener serviceListener) {
        this.serviceListener = serviceListener;
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

    public XMPushServiceListener serviceListener() {
        return serviceListener;
    }

    private static class LazyHolder {
        static Dependencies INSTANCE = new Dependencies();
    }

    public static Dependencies getInstance() {
        return LazyHolder.INSTANCE;
    }
}

