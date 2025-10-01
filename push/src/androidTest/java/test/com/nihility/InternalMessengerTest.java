package test.com.nihility;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.test.core.app.ApplicationProvider;

import com.nihility.InternalMessenger;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class InternalMessengerTest {
    private final static String INTENT_ACTION = "test";
    private final IntentFilter intentFilter = new IntentFilter(INTENT_ACTION);
    private final Context applicationContext = ApplicationProvider.getApplicationContext();
    InternalMessenger sender = new InternalMessenger(applicationContext);
    InternalMessenger receiver = new InternalMessenger(applicationContext);

    @Before
    public void setUp() {
        receiver.register(intentFilter);
    }

    @Test
    public void receiveFromAnotherMessenger() throws InterruptedException {
        CountDownLatch doneSignal = new CountDownLatch(1);
        receiver.addListener(intent -> doneSignal.countDown());

        sender.send(new Intent(INTENT_ACTION));

        assertTrue(doneSignal.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void allListenersAreCalled() throws InterruptedException {
        int listeners = 10;
        CountDownLatch doneSignal = new CountDownLatch(listeners);
        for (int i = 0; i < listeners; i++) {
            receiver.addListener(intent -> doneSignal.countDown());
        }

        sender.send(new Intent(INTENT_ACTION));

        assertTrue(doneSignal.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void receiveByMultipleMessengers() throws InterruptedException {
        int messengerCount = 10;
        CountDownLatch doneSignal = new CountDownLatch(messengerCount);
        for (int i = 0; i < messengerCount; i++) {
            InternalMessenger messenger = new InternalMessenger(applicationContext);
            messenger.register(intentFilter);
            messenger.addListener(intent -> doneSignal.countDown());
        }

        sender.send(new Intent(INTENT_ACTION));

        assertTrue(doneSignal.await(1, TimeUnit.SECONDS));
    }
}