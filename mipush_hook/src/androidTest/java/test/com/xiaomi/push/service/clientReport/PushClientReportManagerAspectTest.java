package test.com.xiaomi.push.service.clientReport;

import static org.junit.Assert.assertTrue;

import com.nihility.Hooked;
import com.xiaomi.push.service.clientReport.PushClientReportManager;

import org.junit.Test;

public class PushClientReportManagerAspectTest {
    @Test
    public void avoidTrackingByAspectJHooked() {
        String arbitrary = "123";
        PushClientReportManager.getInstance(null)
                .reportEvent4ERROR(arbitrary, arbitrary, arbitrary, arbitrary);

        assertTrue(Hooked.contains(PushClientReportManager.class.getSimpleName()));
    }

}