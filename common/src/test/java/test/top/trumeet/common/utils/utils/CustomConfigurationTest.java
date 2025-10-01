package test.top.trumeet.common.utils.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.HashMap;

import top.trumeet.common.utils.CustomConfiguration;

public class CustomConfigurationTest {

    @Test
    public void textIcon() {
        CustomConfiguration custom = new CustomConfiguration(new HashMap<>() {{
            put("__mi_push_text_icon", "qwe");
        }});

        assertEquals("qwe", custom.textIcon(null));
    }
}