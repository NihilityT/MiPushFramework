package test.com.xiaomi.push.service.service;

import static com.xiaomi.push.service.PullAllApplicationDataFromServerJob.getPullAction;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;

import com.nihility.XMPushUtils;
import com.xiaomi.channel.commonutils.reflect.JavaCalls;
import com.xiaomi.push.service.MiPushMessageDuplicate;
import com.xiaomi.push.service.MiPushMessageDuplicateAspect;
import com.xiaomi.push.service.XMPushService;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmpush.thrift.XmPushActionNotification;

import org.junit.Test;

public class MiPushMessageDuplicateAspectTest {

    private static final XMPushService xmPushService = new XMPushService() {{
        JavaCalls.setField(this, "mBase", ApplicationProvider.getApplicationContext());
    }};

    @Test
    public void bypassMockedMiPushMessageDuplicationCheck() {
        XmPushActionNotification pullAction = getPullAction("appId");
        XmPushActionContainer container = XMPushUtils.packToContainer(pullAction, "");

        String id = pullAction.getId();
        ensureAlreadyDuplication(id);

        MiPushMessageDuplicateAspect.markAsMock(container);
        assertFalse(checkDuplicationForId(id));
    }

    @Test
    public void resumeDuplicationStateAfterMocked() {
        String id = "message id";
        ensureAlreadyDuplication(id);

        MiPushMessageDuplicateAspect.mockId = "message id";
        checkDuplicationForId(id);

        assertTrue(checkDuplicationForId(id));
    }

    private static void ensureAlreadyDuplication(String id) {
        checkDuplicationForId(id);
        assertTrue(checkDuplicationForId(id));
    }

    private static boolean checkDuplicationForId(String messageId) {
        return MiPushMessageDuplicate.isDuplicateMessage(xmPushService,
                "package name", messageId);
    }

}