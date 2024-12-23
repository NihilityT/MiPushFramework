package com.nihility.service;

import com.xiaomi.push.service.ForegroundHelper;

public class ForegroundAbility implements XMPushServiceListener {
    public final ForegroundHelper foregroundHelper;

    public ForegroundAbility(ForegroundHelper foregroundHelper) {
        this.foregroundHelper = foregroundHelper;
    }

    @Override
    public void created() {
        foregroundHelper.startForeground();
    }

    @Override
    public void destroy() {
        foregroundHelper.stopForegroundNotification();
    }
}
