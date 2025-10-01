package com.xiaomi.push.service;

import com.nihility.XMPushUtils;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

public class XmPushActionOperator {
    private final XMPushService xmPushService;

    public XmPushActionOperator(XMPushService xmPushService) {
        this.xmPushService = xmPushService;
    }

    public void sendMessage(XmPushActionContainer sendMsgContainer, String packageName) {
        byte[] msgBytes = XMPushUtils.packToBytes(sendMsgContainer);
        xmPushService.sendMessage(packageName, msgBytes, false);
    }

}