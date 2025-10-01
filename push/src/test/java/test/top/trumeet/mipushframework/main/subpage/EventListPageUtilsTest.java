package test.top.trumeet.mipushframework.main.subpage;

import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import org.junit.Test;

import java.util.Set;

import top.trumeet.mipushframework.main.subpage.EventListPageUtils;

public class EventListPageUtilsTest {

    @Test
    public void getStatusShouldReturnMutableSet() {
        EventListPageUtils utils = new EventListPageUtils(null) {
            @Override
            protected boolean isNotificationDisabled(XmPushActionContainer container) {
                return true;
            }
        };

        {
            Set<String> set = utils.getStatus(null);
            set.add("test");
        }
        {
            XmPushActionContainer container = new XmPushActionContainer();
            Set<String> set = utils.getStatus(container);
            set.add("test");
        }
    }
}