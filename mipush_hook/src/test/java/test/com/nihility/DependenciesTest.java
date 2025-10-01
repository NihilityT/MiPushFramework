package test.com.nihility;

import com.nihility.Dependencies;

import org.junit.Test;

public class DependenciesTest {
    @Test(expected = NullPointerException.class)
    public void checkThrowNullPointerExceptionIfDependenciesNotInitialized() {
            Dependencies.getInstance().check();
    }
}