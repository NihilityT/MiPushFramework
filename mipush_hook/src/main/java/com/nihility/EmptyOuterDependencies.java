package com.nihility;

import com.nihility.service.XMPushServiceListener;
import com.xiaomi.push.service.XMPushService;

public class EmptyOuterDependencies implements OuterDependencies {

    @Override
    public Configurations configuration() {
        return null;
    }

    @Override
    public XMPushServiceListener serviceListener(XMPushService pushService) {
        return null;
    }

    @Override
    public HookedMethodHandler hookedMethodHandler() {
        return null;
    }
}
