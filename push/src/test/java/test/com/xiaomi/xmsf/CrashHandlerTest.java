package test.com.xiaomi.xmsf;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.xiaomi.xmsf.CrashHandler;

import org.junit.Test;

public class CrashHandlerTest {

    @Test
    public void collectUncaughtException() throws InterruptedException {
        final Throwable[] throwable = {null};
        CrashHandler.install((t, e) -> throwable[0] = e);

        runExceptionThread();

        assertNotNull(throwable[0]);
    }

    @Test
    public void uninstallHandler() throws InterruptedException {
        final Throwable[] throwable = {null};
        CrashHandler.install((t, e) -> throwable[0] = e);
        CrashHandler.uninstall();

        runExceptionThread();

        assertNull(throwable[0]);
    }

    @Test
    public void invokeDefaultUncaughtExceptionHandler() throws InterruptedException {
        final Throwable[] throwable = {null};
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> throwable[0] = e);
        CrashHandler.initDefaultHandler();
        CrashHandler.install((t, e) -> {});

        runExceptionThread();

        assertNotNull(throwable[0]);
    }

    @Test
    public void uninstallWillResetToDefaultUncaughtHandler() throws InterruptedException {
        final Throwable[] throwable = {null};
        CrashHandler.install((t, e) -> throwable[0] = e);
        CrashHandler.install((t, e) -> throwable[0] = e);
        CrashHandler.uninstall();

        runExceptionThread();

        assertNull(throwable[0]);
    }

    private static void runExceptionThread() throws InterruptedException {
        Thread testThread = new Thread(() -> {
            throw new RuntimeException("Test Exception");
        });
        testThread.start();
        testThread.join();
    }

}