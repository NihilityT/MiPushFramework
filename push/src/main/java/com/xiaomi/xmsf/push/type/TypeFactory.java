package com.xiaomi.xmsf.push.type;

import android.annotation.SuppressLint;

import com.xiaomi.xmpush.thrift.ActionType;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmpush.thrift.XmPushThriftSerializeUtils;
import com.xiaomi.xmsf.utils.ConvertUtils;

import top.trumeet.mipush.provider.event.Event;
import top.trumeet.mipush.provider.event.EventType;
import top.trumeet.mipush.provider.event.type.NotificationType;
import top.trumeet.mipush.provider.event.type.RegistrationResultType;
import top.trumeet.mipush.provider.event.type.UnknownType;

/**
 * Created by Trumeet on 2018/2/7.
 */

public class TypeFactory {
    @SuppressLint("WrongConstant")
    private static @Event.Type int getTypeId (ActionType type) {
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

    public static EventType create (XmPushActionContainer buildContainer,
                                    String pkg) {
        ActionType rawType = buildContainer.getAction();
        int type = getTypeId(rawType);
        String info = String.valueOf(ConvertUtils.toJson(buildContainer));
        byte[] payload = XmPushThriftSerializeUtils.convertThriftObjectToBytes(buildContainer);
        switch (buildContainer.getAction()) {
            case Command:
                break;
            case SendMessage:
                NotificationType eventType = new NotificationType(info, pkg, buildContainer.getMetaInfo().getTitle(),
                        buildContainer.getMetaInfo().getDescription(), payload);
                eventType.setType(Event.Type.SendMessage);
                return eventType;
            case Notification:
                return new NotificationType(info, pkg, buildContainer.getMetaInfo().getTitle(),
                        buildContainer.getMetaInfo().getDescription(), payload);
            case Registration:
                return new RegistrationResultType(info, pkg, payload);
        }
        return new UnknownType(type, info, pkg, payload);
    }
}
