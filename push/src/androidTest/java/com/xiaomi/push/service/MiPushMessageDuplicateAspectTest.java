package com.xiaomi.push.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;

import com.xiaomi.channel.commonutils.reflect.JavaCalls;

import org.junit.Test;

public class MiPushMessageDuplicateAspectTest {

    private static final XMPushService xmPushService = new XMPushService() {{
        JavaCalls.setField(this, "mBase", ApplicationProvider.getApplicationContext());
    }};

    @Test
    public void bypassMockedMiPushMessageDuplicationCheck() {
        String id = "message id";
        ensureAlreadyDuplication(id);

        MiPushMessageDuplicateAspect.mockId = "message id";
        assertFalse(checkDuplicationForId(id));
    }

    @Test
    public void resumeDuplicationStateAfterMocked() {
        String id = "message id";
        ensureAlreadyDuplication(id);

        MiPushMessageDuplicateAspect.mockId = "message id";
        checkDuplicationForId(id);

        assertTrue(checkDuplicationForId(id));
    }

    private static void ensureAlreadyDuplication(String id) {
        checkDuplicationForId(id);
        assertTrue(checkDuplicationForId(id));
    }

    private static boolean checkDuplicationForId(String messageId) {
        return MiPushMessageDuplicate.isDuplicateMessage(xmPushService,
                "package name", messageId);
    }

}