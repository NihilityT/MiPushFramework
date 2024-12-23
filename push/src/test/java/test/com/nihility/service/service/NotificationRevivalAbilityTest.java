package test.com.nihility.service.service;

import static org.mockito.Mockito.verify;

import com.nihility.service.NotificationsRevivalAbility;
import com.xiaomi.push.revival.NotificationsRevivalForSelfUpdated;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NotificationRevivalAbilityTest {
    @Mock
    NotificationsRevivalForSelfUpdated notificationsRevival;
    @InjectMocks
    NotificationsRevivalAbility listener;

    @Test
    public void listenUpdateEventForReviveNotificationsAfterCreated() {
        listener.created();

        verify(notificationsRevival).initialize();
    }

    @Test
    public void stopListenAtDestroy() {
        listener.destroy();

        verify(notificationsRevival).close();
    }
}
