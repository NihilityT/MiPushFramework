package com.nihility.utils;

import android.widget.Toast;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.nihility.XMPushUtils;
import com.xiaomi.channel.commonutils.reflect.JavaCalls;
import com.xiaomi.push.service.MIPushEventProcessor;
import com.xiaomi.push.service.MiPushMessageDuplicateAspect;
import com.xiaomi.push.service.XMPushService;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import java.lang.reflect.InvocationTargetException;

import top.trumeet.common.utils.Utils;

public class MockMIPushMessage {
    private static final String TAG = MockMIPushMessage.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    public static void mockProcessMIPushMessage(XMPushService pushService, XmPushActionContainer container) {
        try {
            MiPushMessageDuplicateAspect.markAsMock(container);
            invokeProcessMiPushMessage(pushService, container);
        } catch (Exception e) {
            logger.e("mock notification failure: ", e);
            Utils.makeText(pushService, "failure", Toast.LENGTH_SHORT);
        }
    }

    public static void invokeProcessMiPushMessage(XMPushService pushService, XmPushActionContainer container) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        byte[] mockDecryptedContent = XMPushUtils.packToBytes(container);
        invokeProcessMiPushMessage(pushService, mockDecryptedContent);
    }

    public static void invokeProcessMiPushMessage(XMPushService pushService, byte[] mockDecryptedContent) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        JavaCalls.<Boolean>callStaticMethodOrThrow(MIPushEventProcessor.class.getName(), "processMIPushMessage",
                pushService, mockDecryptedContent, (long) mockDecryptedContent.length);
    }
}
