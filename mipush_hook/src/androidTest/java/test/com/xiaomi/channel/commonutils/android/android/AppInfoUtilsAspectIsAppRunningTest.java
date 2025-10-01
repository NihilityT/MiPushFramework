package test.com.xiaomi.channel.commonutils.android.android;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.com.xiaomi.channel.commonutils.android.AppInfoUtilsAspect;
import com.xiaomi.channel.commonutils.android.AppInfoUtils;
import com.xiaomi.push.service.PushConstants;
import com.xiaomi.xmpush.thrift.PushMetaInfo;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

public class AppInfoUtilsAspectIsAppRunningTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        AppInfoUtilsAspect.setLastMetaInfo(null);
    }

    @Test
    public void keepDefaultBehavior() {
        String packageName = context.getPackageName();

        assertTrue(AppInfoUtils.isAppRunning(context, packageName));
    }

    @Test
    public void returnFalseIfAppNotRunningAndMetaInfoDoNotContainsAwakeField() {
        AppInfoUtilsAspect.setLastMetaInfo(new PushMetaInfo());

        assertFalse(AppInfoUtils.isAppRunning(context, "arbitrarily"));
    }

    @Test
    public void returnTrueIfAwakeFieldInMetaInfoIsTrue() {
        PushMetaInfo metaInfo = new PushMetaInfo();
        metaInfo.extra = new HashMap<>();
        metaInfo.extra.put(PushConstants.EXTRA_PARAM_AWAKE, Boolean.toString(true));
        AppInfoUtilsAspect.setLastMetaInfo(metaInfo);

        assertTrue(AppInfoUtils.isAppRunning(context, "arbitrarily"));
    }

    @Test
    public void returnTrueIfIsSystemApp() {
        assertTrue(AppInfoUtils.isAppRunning(context, "android"));
    }
}
