package com.nihility;

import com.xiaomi.xmpush.thrift.XmPushActionContainer;

public class MiPushEventListener {
    private static class LazyHolder {
        volatile static MiPushEventListener INSTANCE = new MiPushEventListener();
    }

    private static MiPushEventListener instance;

    public static MiPushEventListener instance() {
        return instance != null ? instance : LazyHolder.INSTANCE;
    }

    public static void setInstance(MiPushEventListener listener) {
        instance = listener;
    }

    public void receiveFromServer(XmPushActionContainer container) {

    }
}
