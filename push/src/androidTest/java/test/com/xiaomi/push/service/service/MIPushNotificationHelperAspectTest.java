package test.com.xiaomi.push.service.service;

import static org.junit.Assert.assertTrue;

import com.nihility.Hooked;
import com.xiaomi.push.service.MIPushNotificationHelper;

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