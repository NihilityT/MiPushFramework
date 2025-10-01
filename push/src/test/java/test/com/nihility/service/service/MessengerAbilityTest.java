package test.com.nihility.service.service;

import static org.mockito.Mockito.verify;

import com.nihility.service.MessengerAbility;
import com.nihility.service.XMPushServiceListener.ConnectionStatus;
import com.xiaomi.push.service.XMPushServiceMessenger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MessengerAbilityTest {
    @Mock
    XMPushServiceMessenger messenger;
    @InjectMocks
    MessengerAbility listener;

    @Test
    public void broadcastAtConnectionStatusChanged() {
        listener.connectionStatusChanged(ConnectionStatus.connected);

        verify(messenger).notifyConnectionStatusChanged(ConnectionStatus.connected.ordinal());
    }

}
