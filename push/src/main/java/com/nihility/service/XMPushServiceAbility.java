package com.nihility.service;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.P;
import static top.trumeet.common.Constants.TAG_CONDOM;

import android.content.Context;
import android.os.Build;

import com.oasisfeng.condom.CondomContext;
import com.xiaomi.channel.commonutils.reflect.JavaCalls;
import com.xiaomi.push.revival.NotificationsRevivalForSelfUpdated;
import com.xiaomi.push.service.BackgroundActivityStartEnabler;
import com.xiaomi.push.service.PullAllApplicationDataFromServerJob;
import com.xiaomi.push.service.XMPushService;
import com.xiaomi.push.service.XMPushServiceMessenger;
import com.xiaomi.xmsf.push.control.XMOutbound;

import java.util.ArrayList;

public class XMPushServiceAbility implements XMPushServiceListener {
    public static XMPushService xmPushService;
    private final ArrayList<XMPushServiceListener> listeners = new ArrayList<>();

    public void addListener(XMPushServiceListener listener) {
        listeners.add(listener);
    }

    public void initialize(XMPushService pushService) {
        xmPushService = pushService;
        RegistrationRecorder.getInstance().initContext(pushService);
        condomContext(pushService);
        initListeners(pushService);
    }

    private void initListeners(XMPushService pushService) {
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
        addListener(new XMPushServiceListener() {
            @Override
            public void connectionStatusChanged(ConnectionStatus connectionStatus) {
                if (connectionStatus == ConnectionStatus.connected) {
                    pushService.executeJob(new PullAllApplicationDataFromServerJob(pushService));
                }
            }
        });
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

    // todo: 搞清楚这里 hook 了什么，起了什么作用，要不要移到 mipush_hook 中
    private static void condomContext(XMPushService pushService) {
        Context mBase = pushService.getBaseContext();
        JavaCalls.setField(pushService, "mBase",
                CondomContext.wrap(mBase, TAG_CONDOM, XMOutbound.create(mBase, XMPushServiceAbility.class.getSimpleName())));
    }
}
