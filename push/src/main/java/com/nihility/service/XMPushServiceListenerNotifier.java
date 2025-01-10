package com.nihility.service;

import android.content.Intent;

import java.util.ArrayList;

public class XMPushServiceListenerNotifier implements XMPushServiceListener {
    private final ArrayList<XMPushServiceListener> listeners = new ArrayList<>();

    public void addListener(XMPushServiceListener listener) {
        listeners.add(listener);
    }

    @Override
    public void created() {
        for (XMPushServiceListener listener : listeners) {
            listener.created();
        }
    }

    @Override
    public void destroy() {
        for (XMPushServiceListener listener : listeners) {
            listener.destroy();
        }
    }

    @Override
    public void start(Intent intent) {
        for (XMPushServiceListener listener : listeners) {
            listener.start(intent);
        }
    }

    @Override
    public void connectionStatusChanged(ConnectionStatus connectionStatus) {
        for (XMPushServiceListener listener : listeners) {
            listener.connectionStatusChanged(connectionStatus);
        }
    }
}
