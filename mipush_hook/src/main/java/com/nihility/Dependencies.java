package com.nihility;

import com.nihility.service.XMPushServiceListener;

public class Dependencies {
    private Configurations configurations;
    private XMPushServiceListener serviceListener;

    public void init(Configurations configurations) {
        this.configurations = configurations;
    }
    public void init(XMPushServiceListener serviceListener) {
        this.serviceListener = serviceListener;
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

