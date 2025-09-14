package top.trumeet.mipush.provider.event.type;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nihility.XMPushUtils;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import top.trumeet.common.R;
import top.trumeet.mipush.provider.entities.Event;
import top.trumeet.mipush.provider.event.EventType;

/**
 * 对应 {@link top.trumeet.common.event.Event.Type#SendMessage}
 *
 * Created by Trumeet on 2018/2/7.
 */

public class NotificationType extends EventType {
    private final String mNotificationTitle;
    private final String mNotificationDetail;

    public NotificationType(String mInfo, String pkg, byte[] payload) {
        super(Event.Type.Notification, mInfo, pkg, payload);
        XmPushActionContainer container = XMPushUtils.packToContainer(payload);
        if (container != null && container.getMetaInfo() != null) {
            this.mNotificationTitle = container.getMetaInfo().getTitle();
            this.mNotificationDetail = container.getMetaInfo().getDescription();
        } else {
            this.mNotificationTitle = null;
            this.mNotificationDetail = null;
        }
    }

    @Override
    @NonNull
    public CharSequence getTitle (Context context) {
        return mNotificationTitle == null ? super.getTitle(context) :
                mNotificationTitle;
    }


    @Nullable
    @Override
    public CharSequence getSummary(Context context) {
        return mNotificationDetail == null ? context.getString(R.string.event_push) :
                mNotificationDetail;
    }

}
