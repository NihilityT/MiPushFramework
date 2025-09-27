package test.com.nihility;

import static com.xiaomi.push.service.PullAllApplicationDataFromServerJob.getPullAction;
import static org.mockito.Mockito.verify;

import com.elvishew.xlog.XLog;
import com.nihility.MiPushEventListener;
import com.nihility.XMPushUtils;
import com.nihility.utils.MockMIPushMessage;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MiPushEventListenerTest {
    @Mock
    MiPushEventListener eventListener;

    @Before
    public void setup() {
        XLog.init();
        MiPushEventListener.setInstance(eventListener);
    }

    @After
    public void teardown() {
        MiPushEventListener.setInstance(null);
    }

    @Test
    public void triggerReceiveFromServerAtProcessMIPushMessage() {
        XmPushActionContainer container = XMPushUtils.packToContainer(getPullAction("appId"), "");
        try {
            MockMIPushMessage.invokeProcessMiPushMessage(null, container);
        } catch (Throwable ignored) {
        }

        verify(eventListener).receiveFromServer(container);
    }

}