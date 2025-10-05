package test.com.nihility;

import static com.xiaomi.push.service.MIPushEventProcessor.postProcessMIPushMessage;
import static com.xiaomi.push.service.PullAllApplicationDataFromServerJob.getPullAction;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.elvishew.xlog.XLog;
import com.nihility.Global;
import com.nihility.MethodHooker;
import com.nihility.MiPushEventListener;
import com.nihility.XMPushUtils;
import com.nihility.utils.MockMIPushMessage;
import com.xiaomi.channel.commonutils.reflect.JavaCalls;
import com.xiaomi.push.service.MIPushEventProcessor;
import com.xiaomi.push.service.MIPushNotificationHelper;
import com.xiaomi.push.service.XMPushService;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import org.aspectj.lang.ProceedingJoinPoint;
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
    XmPushActionContainer container = XMPushUtils.packToContainer(getPullAction("appId"), "");

    @Before
    public void setup() {
        XLog.init();
        Global.setMiPushEventListener(eventListener);
        Global.setMethodHooker(new MethodHooker() {
            @Override
            public boolean shouldSendBroadcast(ProceedingJoinPoint joinPoint, XMPushService pushService, String packageName, XmPushActionContainer container, PushMetaInfo metaInfo) throws Throwable {
                return true;
            }
        });
    }

    @After
    public void teardown() {
        Global.setMiPushEventListener(null);
        Global.setMethodHooker(null);
    }

    @Test
    public void triggerReceiveFromServerAtProcessMIPushMessage() {
        try {
            MockMIPushMessage.invokeProcessMiPushMessage(null, container);
        } catch (Throwable ignored) {
        }

        verify(eventListener).receiveFromServer(container);
    }

    @Test
    public void triggerTransferToApplicationAtPostProcessMIPushMessageSendBroadcast() {
        XMPushService xmPushService = new XMPushService() {
            @Override
            public Context getApplicationContext() {
                return ApplicationProvider.getApplicationContext();
            }

            @Override
            public Context getBaseContext() {
                return ApplicationProvider.getApplicationContext();
            }
        };
        byte[] payload = XMPushUtils.packToBytes(container);
        Intent intent = MIPushEventProcessor.buildIntent(payload, 0);

        postProcessMIPushMessage(xmPushService, null, payload, intent);

        verify(eventListener).transferToApplication(container);
    }

    @Test
    public void triggerTransferToApplicationAtNotifyPushMessage() {
        try {
            MIPushNotificationHelper.notifyPushMessage(null, container, null);
        } catch (Throwable ignored) {
        }

        verify(eventListener).transferToApplication(container);
    }

    @Test
    public void triggerReceiveFromApplicationAtXMPushServiceOnStart() {
        XMPushService xmPushService = new XMPushService() {
            @Override
            public void executeJob(Job job) {
            }
        };
        Intent intent = new Intent("test");

        xmPushService.onStart(intent, 1);

        verify(eventListener).receiveFromApplication(intent);
    }

    @Test
    public void triggerTransferToServerAtSendMessage() {
        XMPushService xmPushService = new XMPushService();

        {
            Intent intent = new Intent("test");

            JavaCalls.callMethod(xmPushService, "sendMessage", intent);

            verify(eventListener).transferToServer(intent);
        }
        {
            Intent intent = new Intent("qwe");

            JavaCalls.callMethod(xmPushService, "sendMessages", intent);

            verify(eventListener).transferToServer(intent);
        }
    }

}