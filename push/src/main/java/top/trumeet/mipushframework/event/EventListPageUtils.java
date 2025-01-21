package top.trumeet.mipushframework.event;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nihility.service.XMPushServiceAbility;
import com.xiaomi.push.service.MIPushEventProcessorAspect;
import com.xiaomi.xmpush.thrift.XmPushActionCommandResult;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmpush.thrift.XmPushActionNotification;
import com.xiaomi.xmsf.R;
import com.xiaomi.xmsf.push.notification.NotificationChannelManager;
import com.xiaomi.xmsf.push.notification.NotificationController;
import com.xiaomi.xmsf.push.utils.Configurations;
import com.xiaomi.xmsf.push.utils.RegSecUtils;
import com.xiaomi.xmsf.utils.ConfigCenter;
import com.xiaomi.xmsf.utils.ConvertUtils;

import org.apache.thrift.TBase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import top.trumeet.mipush.provider.db.EventDb;
import top.trumeet.mipush.provider.event.Event;
import top.trumeet.mipushframework.main.ApplicationInfoPage;

public class EventListPageUtils {
    private final Context context;

    public EventListPageUtils(Context context) {
        this.context = context;
    }

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

    public static void copyToClipboard(Context context, CharSequence info) {
        ClipboardManager clipboardManager = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.setText(info);
    }

    public static void mockMessage(XmPushActionContainer containerWithRegSec) {
        MIPushEventProcessorAspect.mockProcessMIPushMessage(
                XMPushServiceAbility.xmPushService, containerWithRegSec.deepCopy());
    }

    public static @NonNull String getContent(Event event, XmPushActionContainer containerWithRegSec) {
        try {
            XmPushActionContainer newContainer = containerWithRegSec.deepCopy();
            Configurations.getInstance().handle(event.getPkg(), newContainer);
            return containerToJson(newContainer, event.getRegSec()).toString();
        } catch (Throwable e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    public @NonNull String getReceiveDate(@NonNull Event item) {
        Calendar calendarServer = Calendar.getInstance();
        calendarServer.setTime(new Date(item.getDate()));
        int zoneOffset = calendarServer.get(Calendar.ZONE_OFFSET);
        int dstOffset = calendarServer.get(Calendar.DST_OFFSET);
        calendarServer.add(Calendar.MILLISECOND, (zoneOffset + dstOffset));
        DateFormat formatter = SimpleDateFormat.getDateTimeInstance();

        return context.getString(R.string.date_format_long, formatter.format(calendarServer.getTime()));
    }


    @Nullable
    public String getDecoratedStatus(XmPushActionContainer container) {
        try {
            Set<String> ops = Configurations.getInstance().handle(container.getPackageName(), container);
            String status = container.getMetaInfo().getExtra().get("channel_name");
            if (!NotificationChannelManager.isNotificationChannelEnabled(
                    container.getPackageName(),
                    NotificationController.getExistsChannelId(context,
                            container.metaInfo, container.packageName))) {
                ops.add("disable");
            }
            if (!ops.isEmpty()) {
                status = ops + " " + status;
            }
            return status;
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static CharSequence containerToJson(XmPushActionContainer container, String regSec) {
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();
        return gson.toJson(ConvertUtils.toJson(container, regSec));
    }

    public static void startManagePermissions(Context context, String packageName) {
        startManagePermissions(context, packageName, false);
    }

    public static void startManagePermissions(Context context, String packageName, boolean IGNORE_NOT_REGISTERED) {
        // Issue: This currently allows overlapping opens.
        Intent intent = new Intent(context, ApplicationInfoPage.class)
                .putExtra(ApplicationInfoPage.EXTRA_PACKAGE_NAME, packageName);
        if (IGNORE_NOT_REGISTERED) {
            intent.putExtra(ApplicationInfoPage.EXTRA_IGNORE_NOT_REGISTERED, true);
        }
        context.startActivity(intent);
    }

    public static String getDecoratedSummary(String summary, XmPushActionContainer container) {
        if (container.isSetPushAction()) {
            TBase data = getContainer(container);
            if (data instanceof XmPushActionNotification) {
                return summary + ": "
                        + ((XmPushActionNotification) data).getType();
            } else if (data instanceof XmPushActionCommandResult) {
                return summary + ": "
                        + ((XmPushActionCommandResult) data).getCmdName();
            }
        }
        return summary;
    }

    @Nullable
    public static TBase getContainer(XmPushActionContainer container) {
        try {
            return ConvertUtils.getResponseMessageBodyFromContainer(container, RegSecUtils.getRegSec(container));
        } catch (Exception ignored) {
            return null;
        }
    }

    @NonNull
    String getStatusDescription(@NonNull Event item) {
        switch (item.getResult()) {
            case Event.ResultType.OK:
                return getStatusDescriptionByEvent(item);
            case Event.ResultType.DENY_DISABLED:
                return context.getString(R.string.status_deny_disable);
            case Event.ResultType.DENY_USER:
                return context.getString(R.string.status_deny_user);
            default:
                return "";
        }
    }

    @NonNull
    private String getStatusDescriptionByEvent(@NonNull Event item) {
        do {
            XmPushActionContainer container = RegSecUtils.getContainerWithRegSec(item);
            if (container == null) {
                break;
            }
            if (container.metaInfo.passThrough == 1) {
                return context.getString(R.string.message_type_pass_through);
            }
            if (container.metaInfo.passThrough == 0) {
                return context.getString(R.string.message_type_notification);
            }
        } while (false);
        return "";
    }
}