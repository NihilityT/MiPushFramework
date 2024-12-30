package test.com.xiaomi.clientreport.manager;

import static org.junit.Assert.assertTrue;

import com.nihility.Hooked;
import com.xiaomi.clientreport.manager.ClientReportClient;

import org.junit.Test;

public class ClientReportClientAspectTest {
    @Test
    public void avoidTrackingByAspectJHooked() {
        ClientReportClient.init(null);

        assertTrue(Hooked.contains(ClientReportClient.class.getSimpleName()));
    }
}