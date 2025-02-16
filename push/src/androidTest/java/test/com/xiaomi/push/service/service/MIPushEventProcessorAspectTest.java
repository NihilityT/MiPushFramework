package test.com.xiaomi.push.service.service;

import static com.xiaomi.push.service.MIPushEventProcessor.buildIntent;
import static com.xiaomi.push.service.MIPushEventProcessor.postProcessMIPushMessage;
import static com.xiaomi.push.service.PullAllApplicationDataFromServerJob.getPullAction;
import static com.xiaomi.push.service.XmPushActionOperator.packToBytes;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Intent;

import com.nihility.service.RegistrationRecorder;
import com.xiaomi.channel.commonutils.reflect.JavaCalls;
import com.xiaomi.push.service.MIPushEventProcessor;
import com.xiaomi.push.service.XmPushActionOperator;
import com.xiaomi.push.service.clientReport.ReportConstants;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.InvocationTargetException;

@RunWith(MockitoJUnitRunner.class)
public class MIPushEventProcessorAspectTest {

    private final XmPushActionContainer container = XmPushActionOperator.packToContainer(getPullAction("appId"), "");

    @Test
    public void avoidTrackingByMessageIdAndMessageType() {
        Intent intent = buildIntent(packToBytes(container), 0);

        intent.putExtra("messageId", "123");
        intent.putExtra(ReportConstants.EVENT_MESSAGE_TYPE, ReportConstants.REGISTER_TYPE);

        assertFalse(intent.hasExtra("messageId"));
        assertFalse(intent.hasExtra(ReportConstants.EVENT_MESSAGE_TYPE));
    }

    @Test
    public void recordRegisterInfoInBuildContainer() {
        RegistrationRecorder recorder = mock();
        RegistrationRecorder.setInstance(recorder);
        try {
            postProcessMIPushMessage(null, null, packToBytes(container), null);
        } catch (Throwable ignored) {
        }

        verify(recorder).recordRegSec(container);
    }

    @Test
    public void bypassIsIntentAvailableCheck() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        assertTrue(JavaCalls.callStaticMethodOrThrow(MIPushEventProcessor.class, "isIntentAvailable", null, null));
    }
}