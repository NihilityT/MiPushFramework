package com.nihility.service;

import android.content.Intent;

import com.xiaomi.push.service.XMPushService;

public interface XMPushServiceListener {

    default void initialize(XMPushService pushService) {
    }

    default void created() {
    }

    default void destroy() {
    }

    default void start(Intent intent) {
    }

    enum ConnectionStatus {
        connecting,
        connected,
        disconnected;

        public static ConnectionStatus of(int i) {
            return values()[i];
        }
    }

    default void connectionStatusChanged(ConnectionStatus connectionStatus) {
    }
}
