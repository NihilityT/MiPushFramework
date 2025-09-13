package top.trumeet.mipush.provider.event.type;

import android.annotation.SuppressLint;

import com.nihility.XMPushUtils;
import com.xiaomi.xmpush.thrift.ActionType;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmsf.utils.ConvertUtils;

import top.trumeet.mipush.provider.entities.Event;
import top.trumeet.mipush.provider.event.EventType;

/**
 * Created by Trumeet on 2018/2/7.
 */

public class TypeFactory {
    public static EventType createForStore(XmPushActionContainer buildContainer) {
        String pkg = buildContainer.packageName;
        String info = String.valueOf(ConvertUtils.toJson(buildContainer));
        byte[] payload = XMPushUtils.packToBytes(buildContainer);

        switch (buildContainer.getAction()) {
            case SendMessage:
                NotificationType eventType = new NotificationType(info, pkg, payload);
                eventType.setType(Event.Type.SendMessage);
                return eventType;
            case Notification:
                return new NotificationType(info, pkg, payload);
            case Registration:
                return new RegistrationResultType(info, pkg, payload);
            default:
                return new UnknownType(getTypeId(buildContainer.getAction()), info, pkg, payload);
        }
    }

    public static EventType createForDisplay(Event eventFromDB) {
        String pkg = eventFromDB.getPkg();
        String info = eventFromDB.getInfo();
        byte[] payload = eventFromDB.getPayload();

        switch (eventFromDB.getType()) {
            case Event.Type.Command:
                return new CommandType(info, pkg, payload);
            case Event.Type.Notification:
                return new NotificationType(info, pkg, payload);
            case Event.Type.SendMessage:
                NotificationType type = new NotificationType(info, pkg, payload);
                type.setType(Event.Type.SendMessage);
                return type;
            case Event.Type.Registration:
                return new RegistrationType(info, pkg, payload);
            case Event.Type.RegistrationResult:
                return new RegistrationResultType(info, pkg, payload);
            default:
                return new UnknownType(eventFromDB.getType(), info, pkg, payload);
        }
    }


    @SuppressLint("WrongConstant")
    private static @Event.Type int getTypeId(ActionType type) {
        switch (type) {
            case Command:
                return Event.Type.Command;
            case SendMessage:
                return Event.Type.SendMessage;
            case Notification:
                return Event.Type.Notification;
            case SetConfig:
                return Event.Type.SetConfig;
            case AckMessage:
                return Event.Type.AckMessage;
            case Registration:
                return Event.Type.Registration;
            case Subscription:
                return Event.Type.Subscription;
            case ReportFeedback:
                return Event.Type.ReportFeedback;
            case UnRegistration:
                return Event.Type.UnRegistration;
            case UnSubscription:
                return Event.Type.UnSubscription;
            case MultiConnectionResult:
                return Event.Type.MultiConnectionResult;
            case MultiConnectionBroadcast:
                return Event.Type.MultiConnectionBroadcast;
            default:
                return -1;
        }
    }
}
