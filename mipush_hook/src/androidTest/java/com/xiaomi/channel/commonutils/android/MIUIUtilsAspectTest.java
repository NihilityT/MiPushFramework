package com.xiaomi.channel.commonutils.android;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MIUIUtilsAspectTest {

    @Test
    public void hookAllCountryCodeToCN() {
        assertCountryCodeIsCN("ro.miui.region");
        assertCountryCodeIsCN("persist.sys.oppo.region");
    }

    private static void assertCountryCodeIsCN(String property) {
        String region = MIUIUtils.getProperty(property);
        assertEquals("CN", region);
    }
}