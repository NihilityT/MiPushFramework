package com.xiaomi.push.service;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import com.xiaomi.channel.commonutils.logger.LoggerInterface;
import com.xiaomi.channel.commonutils.logger.MyLog;

import org.junit.Before;
import org.junit.Test;

public class PushHostManagerFactoryAspectTest {
    public static final String cnCode = "countrycode=CN";
    public static final String enCode = "countrycode=EN";
    final String[] url = new String[1];

    @Test
    public void addCountryCodeToConfigurationUrl() {
        PushHostManagerFactory.GslbHttpGet httpGet = new PushHostManagerFactory.GslbHttpGet();

        try {
            httpGet.doGet("http://test/");
        } catch (Exception ignored) {
        }

        assertThat(url[0], containsString(cnCode));
    }

    @Test
    public void replaceCountryCodeToCN() {
        PushHostManagerFactory.GslbHttpGet httpGet = new PushHostManagerFactory.GslbHttpGet();

        try {
            httpGet.doGet("http://test/?" + enCode);
        } catch (Exception ignored) {
        }

        assertThat(url[0], not(containsString(enCode)));
        assertThat(url[0], containsString(cnCode));
    }

    @Before
    public void recordUrl() {
        MyLog.setLogLevel(MyLog.INFO);
        MyLog.setLogger(new LoggerInterface() {
            @Override
            public void log(String s) {
                if (s.contains("fetch bucket from :")) {
                    url[0] = s;
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