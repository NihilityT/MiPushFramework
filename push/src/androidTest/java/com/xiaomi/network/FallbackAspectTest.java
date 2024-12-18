package com.xiaomi.network;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.annotation.NonNull;

import com.xiaomi.smack.ConnectionConfiguration;
import com.xiaomi.xmsf.utils.ConfigCenter;

import org.junit.Test;

import java.util.ArrayList;

public class FallbackAspectTest {

    @Test
    public void useUserDefinedXmppServerHostFirst() {
        String expectedHost = "hooked.host";
        ConfigCenter.setInstance(new ConfigCenter() {
            @Override
            public String getXMPPServer(Context ctx) {
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