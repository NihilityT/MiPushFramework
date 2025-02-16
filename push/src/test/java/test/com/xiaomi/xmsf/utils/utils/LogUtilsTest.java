package test.com.xiaomi.xmsf.utils.utils;

import static org.junit.Assert.assertEquals;

import com.xiaomi.xmsf.utils.LogUtils;

import org.junit.Test;

import java.util.Date;

public class LogUtilsTest {

    @Test
    public void ensureLogArchiveNamedByDate() {
        String name = LogUtils.logArchiveName(new Date(2025 - 1900, 2, 1, 1, 2, 3));
        assertEquals("logs_2025-03-01_01-02-03", name);
    }
}