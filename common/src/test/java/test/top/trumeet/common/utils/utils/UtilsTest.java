package test.top.trumeet.common.utils.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Date;

import top.trumeet.common.utils.Utils;

public class UtilsTest {

    @Test
    public void getUTC() {
        Date date = new Date(0);
        assertEquals(Utils.getUTC(date).getTime(), 0);
    }
}