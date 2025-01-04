package test.com.nihility.service.service;

import static org.mockito.Mockito.verify;

import com.nihility.service.ForegroundAbility;
import com.nihility.service.ForegroundHelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ForegroundAbilityTest {
    @Mock
    ForegroundHelper foregroundHelper;
    @InjectMocks
    ForegroundAbility listener;

    @Test
    public void ensureServiceForegroundAfterCreated() {
        listener.created();

        verify(foregroundHelper).startForeground();
    }

    @Test
    public void stopServiceForegroundAfterDestroy() {
        listener.destroy();

        verify(foregroundHelper).stopForegroundNotification();
    }
}
