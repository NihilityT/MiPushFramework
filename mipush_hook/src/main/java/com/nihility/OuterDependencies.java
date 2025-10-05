package com.nihility;

import com.nihility.service.XMPushServiceListener;
import com.xiaomi.push.service.XMPushService;

public interface OuterDependencies {
    Configurations configuration();

    XMPushServiceListener serviceListener(XMPushService pushService);

    HookedMethodHandler hookedMethodHandler();
}
