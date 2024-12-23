package com.nihility.service;

import android.content.Intent;

public interface XMPushServiceListener {
    default void created() {
    }

    default void destroy() {
    }

    default void start(Intent intent) {
    }

    enum ConnectionStatus {
        connecting,
        connected,
        disconnected,
    }

    default void connectionStatusChanged(ConnectionStatus connectionStatus) {
    }
}
