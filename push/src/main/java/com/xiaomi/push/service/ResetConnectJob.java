package com.xiaomi.push.service;

import com.xiaomi.channel.commonutils.reflect.JavaCalls;
import com.xiaomi.network.Fallback;
import com.xiaomi.network.HostManager;
import com.xiaomi.smack.ConnectionConfiguration;

class ResetConnectJob extends XMPushService.Job {

    private final XMPushService xmPushService;

    public ResetConnectJob(XMPushService xmPushService) {
        super(XMPushService.Job.TYPE_RESET_CONNECT);
        this.xmPushService = xmPushService;
    }

    @Override
    public String getDesc() {
        return "reset connection";
    }

    @Override
    public void process() {
        Fallback fallback = HostManager.getInstance().getFallbacksByHost(ConnectionConfiguration.getXmppServerHost(), false);
        JavaCalls.setField(fallback, "timestamp", 0);
        HostManager.getInstance().getFallbacksByHost(ConnectionConfiguration.getXmppServerHost(), true);
        xmPushService.disconnect(11, null);
        xmPushService.scheduleConnect(true);
    }
}
