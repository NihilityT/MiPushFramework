package com.xiaomi.push.service;

import static org.junit.Assert.assertTrue;

import com.nihility.Hooked;

import org.junit.Test;

public class MIPushNotificationHelperAspectTest {

    @Test
    public void notifyPushMessage() {
        try {
            MIPushNotificationHelper.notifyPushMessage(null, null, null);
        } catch (Throwable ignored) {
        }
        assertTrue(Hooked.contains("MIPushNotificationHelper.NotifyPushMessageInfo"));
    }
}