import android.content.Context;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import com.xiaomi.channel.commonutils.android.DeviceInfo;
import com.xiaomi.channel.commonutils.android.MIUIUtils;
import com.xiaomi.smack.ConnectionConfiguration;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

public class Hook implements Initializer<Void> {
    @NonNull
    @Override
    public Void create(@NonNull Context context) {
        try {
            doHook();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.emptyList();
    }


    private void doHook() throws Exception {
        fakeMIUISystem();
        avoidTracking();
        ensureDefaultXmppServerIsCnHost();
    }

    private void fakeMIUISystem() throws Exception {
        hookField(MIUIUtils.class, "isMIUI", MIUIUtils.IS_MIUI);
    }

    private void avoidTracking() throws Exception {
        hookField(DeviceInfo.class, "sCachedIMEI", "");
    }

    private static void ensureDefaultXmppServerIsCnHost() {
        ConnectionConfiguration.setXmppServerHost(ConnectionConfiguration.XMPP_SERVER_CHINA_HOST_P);
    }

    private void hookField(Class klass, String field, Object value) throws Exception {
        Field target = klass.getDeclaredField(field);
        target.setAccessible(true);
        target.set(null, value);
    }
}
