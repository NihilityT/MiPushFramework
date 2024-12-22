package com.xiaomi.push.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.nihility.InternalMessenger;
import com.xiaomi.smack.Connection;

public class XMPushServiceMessenger extends InternalMessenger {
    public final static String IntentGetConnectionStatus = "getConnectionStatus";
    public final static String IntentSetConnectionStatus = "setConnectionStatus";
    public final static String IntentStartForeground = "startForeground";

    private final XMPushService xmPushService;
    private int connectionStatus;

    XMPushServiceMessenger(XMPushService context) {
        super(context);
        this.xmPushService = context;
        register(new IntentFilter(IntentGetConnectionStatus));
        register(new IntentFilter(PushConstants.ACTION_RESET_CONNECTION));
        register(new IntentFilter(IntentStartForeground));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        handle(intent);
        notifyConnectionStatusChanged(connectionStatus);
    }

    public void notifyConnectionStatusChanged(int connectionStatus) {
        this.connectionStatus = connectionStatus;
        send(setConnectionStatusIntent(getDesc(connectionStatus)));
    }

    private @NonNull Intent setConnectionStatusIntent(String connectionStatus) {
        Intent intent = new Intent(IntentSetConnectionStatus);
        intent.putExtra("status", connectionStatus);
        Connection currentConnection = xmPushService.getCurrentConnection();
        if (currentConnection != null) {
            intent.putExtra("host", currentConnection.getHost());
        }
        return intent;
    }

    private void handle(Intent intent) {
        if (TextUtils.equals(intent.getAction(), PushConstants.ACTION_RESET_CONNECTION)) {
            resetConnection();
        } else if (TextUtils.equals(intent.getAction(), IntentStartForeground)) {
            new ForegroundHelper(xmPushService).startForeground();
        }
    }

    private void resetConnection() {
        xmPushService.executeJob(new ResetConnectJob(xmPushService));
    }

    String getDesc(int var1) {
        switch (var1) {
            case 0:
                return "connecting";
            case 1:
                return "connected";
            case 2:
                return "disconnected";
            default:
                return "unknown";
        }
    }
}
