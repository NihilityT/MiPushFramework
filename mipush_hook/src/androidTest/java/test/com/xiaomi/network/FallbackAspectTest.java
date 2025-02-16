package test.com.xiaomi.network;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;

import com.nihility.Configurations;
import com.nihility.Dependencies;
import com.xiaomi.network.Fallback;
import com.xiaomi.smack.ConnectionConfiguration;

import org.junit.Test;

import java.util.ArrayList;

public class FallbackAspectTest {

    @Test
    public void useUserDefinedXmppServerHostFirst() {
        String expectedHost = "hooked.host";
        Dependencies.getInstance().init(new Configurations() {
            @Override
            public String getXMPPServer() {
                return expectedHost;
            }
        });
        Fallback fallback = getXmppServerFallback();

        ArrayList<String> hosts = fallback.getHosts();

        assertEquals(expectedHost, hosts.get(0));
    }

    private static @NonNull Fallback getXmppServerFallback() {
        return new Fallback(ConnectionConfiguration.getXmppServerHost());
    }
}