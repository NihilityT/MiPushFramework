package test.com.nihility.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.nihility.utils.Singleton;

import org.junit.Test;

public class SingletonTest {
    static class Foo {
    }

    @Test
    public void classToCreateMustHaveDefaultConstructor() {
        // create by return type
        Foo foo = Singleton.instance();

        // create by explicit type
        Singleton.<Foo>instance();
    }

    @Test
    public void returnSameInstance() {
        Foo foo = Singleton.instance();
        assertEquals(foo, Singleton.<Foo>instance());
    }

    @Test
    public void shouldNotPassParameterToInstanceMethod() {
        assertThrows(IllegalArgumentException.class, () -> Singleton.instance((Foo) null));
    }

    @Test
    public void supportUserDefinedInstance() {
        Foo foo = Singleton.instance();

        Foo tmp = new Foo();
        Singleton.reset(tmp);
        assertEquals(tmp, Singleton.<Foo>instance());

        Singleton.<Foo>reset();
        assertEquals(foo, Singleton.<Foo>instance());
    }

    @Test
    public void autoResetUserDefinedInstance() {
        Foo foo = Singleton.instance();

        Foo tmp = new Foo();
        try (Singleton.AutoReset ignored = Singleton.reset(tmp)) {
            assertEquals(tmp, Singleton.<Foo>instance());
        }
        assertEquals(foo, Singleton.<Foo>instance());
    }

    static class SlowFoo {
        SlowFoo() {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Test
    public void supportMultipleThreadAccess() throws InterruptedException {
        int testSize = 3;
        Thread[] threads = new Thread[testSize];
        SlowFoo[] slowFoos = new SlowFoo[testSize];
        for (int i = 0; i < testSize; ++i) {
            int finalI = i;
            threads[i] = new Thread(() -> slowFoos[finalI] = Singleton.instance());
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        for (SlowFoo SlowFoo : slowFoos) {
            assertEquals(slowFoos[0], SlowFoo);
        }
    }
}