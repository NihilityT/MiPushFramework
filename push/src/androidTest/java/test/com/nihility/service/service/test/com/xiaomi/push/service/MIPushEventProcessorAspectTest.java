package test.com.nihility.service.service.test.com.xiaomi.push.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.xiaomi.channel.commonutils.reflect.JavaCalls;
import com.xiaomi.push.service.MIPushEventProcessor;
import com.xiaomi.push.service.PushConstants;
import com.xiaomi.push.service.XMPushService;
import com.xiaomi.smack.Connection;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmsf.push.utils.Configurations;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
public class MIPushEventProcessorAspectTest {
    private boolean reportCheckAlive = false;
    private final XmPushActionContainer container = new XmPushActionContainer() {
        @Override
        public String getPackageName() {
            reportCheckAlive = true;
            return super.getPackageName();
        }
    };
    private final XMPushService service = new XMPushService() {
        {
            JavaCalls.setField(this, "mBase", ApplicationProvider.getApplicationContext());
        }

        @Override
        public Connection getCurrentConnection() {
            return null;
        }
    };

    @Test
    public void defaultToNoAwake() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        assertFalse(shouldAwake(null));

        PushMetaInfo metaInfo = new PushMetaInfo();
        metaInfo.extra = new HashMap<>();
        assertFalse(shouldAwake(metaInfo));

        metaInfo.extra.put("arbitrarily", "arbitrarily");
        assertFalse(shouldAwake(metaInfo));
    }

    @Test
    public void awakeIfAwakeFieldIsTrue() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        PushMetaInfo metaInfo = new PushMetaInfo();
        metaInfo.extra = new HashMap<>();

        metaInfo.extra.put(PushConstants.EXTRA_PARAM_AWAKE, Boolean.toString(false));
        assertFalse(shouldAwake(metaInfo));

        metaInfo.extra.put(PushConstants.EXTRA_PARAM_AWAKE, Boolean.toString(true));
        assertTrue(shouldAwake(metaInfo));
    }


    @Test
    public void awakeIfAppRunning() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        assertTrue(shouldAwake(null, "android"));
    }

    @Test
    public void awakeIfIsSystemApp() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        assertTrue(shouldAwake(null, "android"));
    }

    @Test
    public void keepBehaviorForReportCheckAlive() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        PushMetaInfo metaInfo = new PushMetaInfo();
        metaInfo.setId("arbitrarily");
        metaInfo.extra = new HashMap<>();
        metaInfo.extra.put(PushConstants.EXTRA_PARAM_CHECK_ALIVE, "arbitrarily");
        metaInfo.extra.put(PushConstants.EXTRA_PARAM_AWAKE, "arbitrarily");

        shouldAwake(metaInfo);
        assertTrue(reportCheckAlive);
    }

    @Test
    public void awakeIfAwakeFieldIsTrueByConfigure() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, JSONException {
        String packageName = "arbitrary";

        PushMetaInfo metaInfo = new PushMetaInfo();
        metaInfo.extra = new HashMap<>();

        assertFalse(shouldAwake(metaInfo, packageName));

        setAllowAwakeByConfigurationFor(packageName);
        assertTrue(shouldAwake(metaInfo, packageName));
    }

    private static void setAllowAwakeByConfigurationFor(String packageName) throws JSONException {
        Configurations.getInstance().load("{\n" +
                "  \"version\": \"0.1.0\",\n" +
                "  \"configs\": {\n" +
                "    \"^\": [\n" +
                "      [\n" +
                "        \"cond\",\n" +
                "        [\n" +
                "          {\n" +
                "            \"description\": \"使用 | 来分隔不同的包名\",\n" +
                "            \"match\": {\n" +
                "              \"packageName\": \""+ packageName +"\"\n" +
                "            }\n" +
                "          },\n" +
                "          {\n" +
                "            \"replace\": {\n" +
                "              \"metaInfo\": {\n" +
                "                \"extra\": {\n" +
                "                  \"__awake\": \"true\"\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"stop\": false\n" +
                "          }\n" +
                "        ]\n" +
                "      ]\n" +
                "    ]\n" +
                "  }\n" +
                "}");
    }

    private boolean shouldAwake(PushMetaInfo metaInfo) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String packageName = "test";
        return shouldAwake(metaInfo, packageName);
    }

    private boolean shouldAwake(PushMetaInfo metaInfo, String packageName) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        container.setPackageName(packageName);
        container.metaInfo = metaInfo;
        return JavaCalls.callStaticMethodOrThrow(MIPushEventProcessor.class, "shouldSendBroadcast",
                service, packageName, container, metaInfo);
    }
}
