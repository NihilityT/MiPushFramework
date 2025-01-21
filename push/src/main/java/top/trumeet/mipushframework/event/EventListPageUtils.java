package top.trumeet.mipushframework.event;

import com.xiaomi.xmsf.utils.ConfigCenter;

import java.util.List;
import java.util.Set;

import top.trumeet.mipush.provider.db.EventDb;
import top.trumeet.mipush.provider.event.Event;

public class EventListPageUtils {
    static List<Event> getEvents(int pageIndex, int pageSize, String packetName, String query) {
        Set<Integer> types = null;
        if (!ConfigCenter.getInstance().isShowAllEvents()) {
            types = Set.of(
                    Event.Type.SendMessage,
                    Event.Type.Registration,
                    Event.Type.RegistrationResult,
                    Event.Type.UnRegistration);
        }
        return EventDb.queryByPage(pageIndex, pageSize,
                types, packetName, query);
    }
}