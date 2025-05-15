package top.trumeet.mipush.provider.event.type;

import top.trumeet.mipush.provider.entities.Event;
import top.trumeet.mipush.provider.event.EventType;

/**
 * Created by Trumeet on 2018/2/7.
 */

public class TypeFactory {
    public static EventType create (Event event, String pkg) {
        switch (event.getType()) {
            case Event.Type.Command:
                return new CommandType(event.getInfo(),
                        pkg, event.getPayload());
            case Event.Type.Notification:
                return new NotificationType(event.getInfo(), pkg, event.getNotificationTitle(),
                        event.getNotificationSummary(), event.getPayload());
            case Event.Type.SendMessage:
                NotificationType type = new NotificationType(event.getInfo(), pkg, event.getNotificationTitle(),
                        event.getNotificationSummary(), event.getPayload());
                type.setType(Event.Type.SendMessage);
                return type;
            case Event.Type.Registration:
                return new RegistrationType(event.getInfo(),
                        pkg, event.getPayload());
            case Event.Type.RegistrationResult:
                return new RegistrationResultType(event.getInfo(),
                        pkg, event.getPayload());
            default:
                return new UnknownType(event.getType(), event.getInfo(), pkg, event.getPayload());
        }
    }
}
