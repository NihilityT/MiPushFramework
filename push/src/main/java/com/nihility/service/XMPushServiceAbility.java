package com.nihility.service;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.P;

import android.os.Build;

import com.xiaomi.push.revival.NotificationsRevivalForSelfUpdated;
import com.xiaomi.push.service.BackgroundActivityStartEnabler;
import com.xiaomi.push.service.ForegroundHelper;
import com.xiaomi.push.service.RegisterRecorder;
import com.xiaomi.push.service.XMPushService;
import com.xiaomi.push.service.XMPushServiceMessenger;

import java.util.ArrayList;

public class XMPushServiceAbility implements XMPushServiceListener {
    private final ArrayList<XMPushServiceListener> listeners = new ArrayList<>();

    public void addListener(XMPushServiceListener listener) {
        listeners.add(listener);
    }

    public void initialize(XMPushService pushService) {
        addListener(new RegisterRecordAbility(new RegisterRecorder(pushService)));
        addListener(new ForegroundAbility(new ForegroundHelper(pushService)));
        addListener(new MessengerAbility(new XMPushServiceMessenger(pushService)));
        if (SDK_INT > P) {
            addListener(new XMPushServiceListener() {
                @Override
                public void created() {
                    BackgroundActivityStartEnabler.initialize(pushService);
                }
            });
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            addListener(new NotificationsRevivalAbility(new NotificationsRevivalForSelfUpdated(pushService, sbn -> sbn.getTag() == null)));
        }
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
    public void connectionStatusChanged(ConnectionStatus connectionStatus) {
        for (XMPushServiceListener listener : listeners) {
            listener.connectionStatusChanged(connectionStatus);
        }
    }

}
