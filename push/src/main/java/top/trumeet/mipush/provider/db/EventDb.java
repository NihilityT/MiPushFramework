package top.trumeet.mipush.provider.db;

import static top.trumeet.mipush.provider.DatabaseUtils.daoSession;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.xiaomi.push.service.MIPushEventProcessor;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmpush.thrift.XmPushActionRegistrationResult;
import com.xiaomi.xmsf.push.utils.RegSecUtils;
import com.xiaomi.xmsf.utils.ConvertUtils;

import org.greenrobot.greendao.query.QueryBuilder;
import org.greenrobot.greendao.query.WhereCondition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import top.trumeet.common.utils.DatabaseUtils;
import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.event.Event;
import top.trumeet.mipush.provider.event.EventType;
import top.trumeet.mipush.provider.gen.db.EventDao;

/**
 * @author Trumeet
 * @date 2017/12/23
 */

public class EventDb {
    public static final String AUTHORITY = "top.trumeet.mipush.providers.EventProvider";
    public static final String BASE_PATH = "EVENT";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    private static DatabaseUtils getInstance(Context context) {
        return new DatabaseUtils(CONTENT_URI, context.getContentResolver());
    }

    public static class RegistrationInfo {
        public Set<String> registered = new HashSet<>();
        public Set<String> unregistered = new HashSet<>();
    }

    public static long insertEvent(Event event) {
        if (event.getType() == Event.Type.SendMessage) {
            Utils.setLastReceiveTime(event.getPkg(), event.getDate());
        }
        return daoSession.insert(event);
    }

    public static long insertEvent(@Event.ResultType int result,
                                   EventType type) {
        return insertEvent(type.fillEvent(new Event(null
                , type.getPkg()
                , type.getType()
                , Utils.getUTC().getTime()
                , result
                , type.getInfo()
                , null
                , null
                , type.getPayload()
                , Utils.getRegSec(type.getPkg())
        )));
    }

    public static List<Event> queryByPage(
            int pageIndex, int pageSize,
            @Nullable Set<Integer> types,
            @Nullable String pkg, @Nullable String text) {
        return query((pageIndex - 1) * pageSize, pageSize, types, pkg, text);
    }

    public static List<Event> query(
            int skip, int limit,
            @Nullable Set<Integer> types,
            @Nullable String pkg, @Nullable String text) {
        QueryBuilder<Event> query = daoSession.queryBuilder(Event.class)
                .orderDesc(EventDao.Properties.Date)
                .limit(limit)
                .offset(skip);
        if (pkg != null && !pkg.trim().isEmpty()) {
            query.where(EventDao.Properties.Pkg.eq(pkg));
        }
        if (types != null && !types.isEmpty()) {
            query.where(EventDao.Properties.Type.in(types));
        }
        if (text != null && !text.trim().isEmpty()) {
            query.where(EventDao.Properties.Info.like("%" + text + "%"));
        }
        return query.list();
    }

    public static void deleteHistory() {
        String data = (Utils.getUTC().getTime() - 1000L * 3600L * 24 * 7) + "";
        QueryBuilder<Event> query = daoSession.queryBuilder(Event.class)
                .where(EventDao.Properties.Type.in(Event.Type.RECEIVE_PUSH, Event.Type.REGISTER, Event.Type.Command))
                .where(EventDao.Properties.Date.lt(data));
        query.buildDelete().executeDeleteWithoutDetachingEntities();
    }

    public static RegistrationInfo queryRegistered() {
        QueryBuilder<Event> query = daoSession.queryBuilder(Event.class)
                .where(EventDao.Properties.Type.in(Event.Type.RegistrationResult, Event.Type.UnRegistration))
                .where(new WhereCondition.StringCondition("1" +
                        " GROUP BY " + EventDao.Properties.Pkg.columnName +
                        " HAVING MAX(" + EventDao.Properties.Date.columnName + ")"));
        List<Event> events = query.list();

        RegistrationInfo info = new RegistrationInfo();
        for (Event event : events) {
            XmPushActionContainer container = MIPushEventProcessor.buildContainer(event.getPayload());
            XmPushActionRegistrationResult data = null;
            try {
                data = (XmPushActionRegistrationResult)
                        ConvertUtils.getResponseMessageBodyFromContainer(container,
                                RegSecUtils.getRegSec(container));
            } catch (Exception ignored) {
            }
            if (event.getType() == Event.Type.RegistrationResult && (data == null || data.errorCode == 0)) {
                info.registered.add(event.getPkg());
            } else {
                info.unregistered.add(event.getPkg());
            }
        }
        return info;
    }

    public static long getLastReceiveTime(String packageName) {
        Long time = Utils.getLastReceiveTime(packageName);
        if (time != null) {
            return time;
        }

        HashSet<Integer> types = new HashSet<>();
        types.add(Event.Type.SendMessage);
        List<Event> events = EventDb.query(0, 1, types,
                packageName, null);
        long lastReceiveTime = 0;
        if (!events.isEmpty()) {
            lastReceiveTime = events.get(0).getDate();
        }
        Utils.setLastReceiveTime(packageName, lastReceiveTime);
        return lastReceiveTime;
    }

}
