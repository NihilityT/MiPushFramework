package test.top.trumeet.mipushframework.utils;

import static org.junit.Assert.assertEquals;

import androidx.test.core.app.ApplicationProvider;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.TimeZone;

import top.trumeet.mipushframework.utils.ParseUtils;

public class ParseUtilsTest {
    private static TimeZone defaultTimeZone;
    static String beginOfUTC = "1970年1月1日 上午8:00:00";

    @BeforeClass
    public static void setUp() {
        defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    }
    @AfterClass
    public static void tearDown() {
        TimeZone.setDefault(defaultTimeZone);
    }

    @Test
    public void parseDate() {
        assertEquals(beginOfUTC,  ParseUtils.parseDate(new Date(0)));
    }

    void testFriendlyDate(Date server, Date now, String expected) {
        String friendlyDateString = ParseUtils.getFriendlyDateString(
                server, now, ApplicationProvider.getApplicationContext());
        assertEquals(expected, friendlyDateString);
    }

    @Test
    public void friendlyDate() {
        long min = 1000L * 60;
        long hour = min * 60;
        long day = 24 * hour;

        Date server = new Date(0);
        {
            Date now = new Date(30 * min);
            testFriendlyDate(server, now, "30 分钟前");
        }
        {
            Date now = new Date(hour);
            testFriendlyDate(server, now, "1 小时前");
        }
        {
            Date now = new Date(5 * hour);
            testFriendlyDate(server, now, "5 小时前");
        }
        {
            Date now = new Date(day);
            testFriendlyDate(server, now, "1 天前");
        }
        {
            Date now = new Date(29 * day);
            testFriendlyDate(server, now, "29 天前");
        }
        {
            Date now = new Date(30 * day);
            testFriendlyDate(server, now, beginOfUTC);
        }
    }
}