package test.com.nihility.service.service;

import static com.nihility.service.XMPushServiceListener.ConnectionStatus.connecting;
import static org.mockito.Mockito.verify;

import android.content.Intent;

import com.nihility.service.XMPushServiceListener;
import com.nihility.service.XMPushServiceListenerNotifier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class XMPushServiceListenerNotifierTest {
    @Mock
    XMPushServiceListener listener;
    XMPushServiceListenerNotifier notifier = new XMPushServiceListenerNotifier();

    @Before
    public void setUp() {
        notifier.addListener(listener);
    }

    @Test
    public void invokeListenersForCreated() {
        notifier.created();
        verify(listener).created();
    }

    @Test
    public void invokeListenersForDestroy() {
        notifier.destroy();
        verify(listener).destroy();
    }

    @Test
    public void invokeListenersForStart() {
        Intent intent = new Intent();
        notifier.start(intent);
        verify(listener).start(intent);
    }

    @Test
    public void invokeListenersForConnectionStatusChanged() {
        notifier.connectionStatusChanged(connecting);
        verify(listener).connectionStatusChanged(connecting);
    }
}
