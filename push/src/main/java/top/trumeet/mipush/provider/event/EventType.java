package top.trumeet.mipush.provider.event;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import top.trumeet.mipush.provider.db.EventDb;
import top.trumeet.mipush.provider.entities.Event;

/**
 * 喂给 {@link Event} 的详细信息。
 * 
 * Created by Trumeet on 2018/2/7.
 */

public abstract class EventType {
    @Event.Type
    private int mType;

    /* buildContainer info */
    private final String mInfo;

    private final String pkg;

    private final byte[] payload;

    public EventType(@Event.Type int mType, String mInfo, String pkg, byte[] payload) {
        this.mType = mType;
        this.mInfo = mInfo;
        this.pkg = pkg;
        this.payload = payload;
    }

    @NonNull
    public CharSequence getTitle (Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            return pm.getApplicationInfo(pkg, PackageManager.GET_UNINSTALLED_PACKAGES).loadLabel(pm);
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
            return pkg;
        }
    }

    @Nullable
    public abstract CharSequence getSummary (Context context);

    @Nullable
    public final CharSequence getInfo (Context context) {
        StringBuilder builder = new StringBuilder();
        CharSequence customInfo = getCustomInfo(context);
        if (customInfo != null) {
            builder.append(customInfo);
            builder.append("<br />");
        }
        if (mInfo != null && !mInfo.isEmpty()) {
            builder.append("<strong>Developer Info:</strong><br />");
            builder.append(mInfo);
        }
        String info = builder.toString();
        return info.trim().equals("") ?
                null : Html.fromHtml(info);
    }

    @Nullable
    public String getCustomInfo (Context context) {
        return null;
    }

    public int getType() {
        return mType;
    }
    public void setType(@Event.Type int type) {
        mType = type;
    }

    public String getInfo() {
        return mInfo;
    }

    public byte[] getPayload() {
        return payload;
    }

    /**
     * Only used when type has meta data
     * @param original Event
     * @see EventDb#insertEvent(int, EventType)
     */
    @NonNull
    public Event fillEvent (@NonNull Event original) {
        return original;
    }

    public String getPkg() {
        return pkg;
    }

    @Override
    public String toString() {
        return "EventType{" +
                "mType=" + mType +
                ", mInfo='" + mInfo + '\'' +
                ", pkg='" + pkg + '\'' +
                '}';
    }
}
