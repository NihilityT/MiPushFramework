package com.xiaomi.push.service;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import com.xiaomi.channel.commonutils.logger.LoggerInterface;
import com.xiaomi.channel.commonutils.logger.MyLog;

import org.junit.Test;

import java.io.IOException;

public class PushHostManagerFactoryAspectTest {

    @Test
    public void addCountryCodeToConfigurationUrl() throws IOException {
        final String[] url = new String[1];
        saveUrlTo(url);
        PushHostManagerFactory.GslbHttpGet httpGet = new PushHostManagerFactory.GslbHttpGet();

        try {
            httpGet.doGet("http://test/");
        } catch (Exception ignored) {}

        assertThat(url[0], containsString("countrycode=CN"));
    }

    private static void saveUrlTo(String[] urlLog) {
        MyLog.setLogger(new LoggerInterface() {
            @Override
            public void log(String s) {
                if (s.contains("fetch bucket from :")) {
                    urlLog[0] = s;
                }
            }
            @Override
            public void log(String s, Throwable throwable) {
            }
            @Override
            public void setTag(String s) {
            }
        });
    }
}