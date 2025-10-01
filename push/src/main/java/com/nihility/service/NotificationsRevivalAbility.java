package com.nihility.service;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.xiaomi.push.revival.NotificationsRevivalForSelfUpdated;

@RequiresApi(api = Build.VERSION_CODES.M)
public class NotificationsRevivalAbility implements XMPushServiceListener {
    private final NotificationsRevivalForSelfUpdated notificationsRevival;

    public NotificationsRevivalAbility(NotificationsRevivalForSelfUpdated notificationsRevival) {
        this.notificationsRevival = notificationsRevival;
    }

    @Override
    public void created() {
        notificationsRevival.initialize();
    }

    @Override
    public void destroy() {
        notificationsRevival.close();
    }
}
