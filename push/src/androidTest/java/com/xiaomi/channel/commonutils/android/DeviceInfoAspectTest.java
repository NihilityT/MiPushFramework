package com.xiaomi.channel.commonutils.android;

import static org.junit.Assert.*;

import org.junit.Test;

public class DeviceInfoAspectTest {

    @Test
    public void avoidTracking() {
        assertEquals("", DeviceInfo.getMacAddress(null));
    }
}