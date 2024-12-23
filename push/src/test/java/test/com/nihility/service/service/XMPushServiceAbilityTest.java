package test.com.nihility.service.service;

import static com.nihility.service.XMPushServiceListener.ConnectionStatus.connecting;
import static org.mockito.Mockito.verify;

import com.nihility.service.XMPushServiceAbility;
import com.nihility.service.XMPushServiceListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class XMPushServiceAbilityTest {
    @Mock
    XMPushServiceListener listener;
    XMPushServiceAbility ability = new XMPushServiceAbility();

    @Before
    public void setUp() {
        ability.addListener(listener);
    }

    @Test
    public void invokeListenersForCreated() {
        ability.created();
        verify(listener).created();
    }

    @Test
    public void invokeListenersForDestroy() {
        ability.destroy();
        verify(listener).destroy();
    }

    @Test
    public void invokeListenersForConnectionStatusChanged() {
        ability.connectionStatusChanged(connecting);
        verify(listener).connectionStatusChanged(connecting);
    }
}
