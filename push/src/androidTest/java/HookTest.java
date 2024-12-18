import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.xiaomi.channel.commonutils.android.DeviceInfo;
import com.xiaomi.channel.commonutils.android.MIUIUtils;
import com.xiaomi.smack.ConnectionConfiguration;

import org.junit.Test;

public class HookTest {
    @Test
    public void fakeMIUISystem() {
        assertTrue(MIUIUtils.isMIUI());
        assertEquals(MIUIUtils.IS_MIUI, MIUIUtils.getIsMIUI());
    }

    @Test
    public void avoidTracking() {
        assertEquals("", DeviceInfo.quicklyGetIMEI(null));
        assertEquals("", DeviceInfo.getMacAddress(null));
    }

    @Test
    public void ensureDefaultXmppServerIsCnHost() {
        assertEquals("cn.app.chat.xiaomi.net", ConnectionConfiguration.getXmppServerHost());
    }
}