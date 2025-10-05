package top.trumeet.mipush.provider.event;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nihility.Global;

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
        return Global.ApplicationNameCache().getAppName(context, pkg);
    }

    @Nullable
    public abstract CharSequence getSummary (Context context);

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
