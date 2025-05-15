package top.trumeet.mipush.provider.entities;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Property;
import org.greenrobot.greendao.annotation.Transient;
import org.greenrobot.greendao.annotation.Unique;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Date;

/**
 * Created by Trumeet on 2017/8/26.
 * A registered application. Can 3 types: {@link Type#ALLOW}, {@link Type#DENY}
 * or {@link Type#ASK}.
 * Default is Ask: show a dialog to request push permission.
 * It will auto create using ask type when application register push.
 *
 * This entity will also save application's push permissions.
 *
 * @author Trumeet
 */

@Entity
public class RegisteredApplication implements Parcelable {
    protected RegisteredApplication(Parcel in) {
        if (in.readByte() == 0) {
            id = null;
        } else {
            id = in.readLong();
        }
        packageName = in.readString();
        type = in.readInt();
    }

    public static final Creator<RegisteredApplication> CREATOR = new Creator<RegisteredApplication>() {
        @Override
        public RegisteredApplication createFromParcel(Parcel in) {
            return new RegisteredApplication(in);
        }

        @Override
        public RegisteredApplication[] newArray(int size) {
            return new RegisteredApplication[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (id == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeLong(id);
        }
        parcel.writeString(packageName);
        parcel.writeInt(type);
    }

    @IntDef({Type.ASK, Type.ALLOW, Type.DENY, Type.ALLOW_ONCE})
    @Retention(SOURCE)
    @Target({ElementType.PARAMETER, ElementType.TYPE,
            ElementType.FIELD, ElementType.METHOD})
    public @interface Type {
        int ASK = 0;
        int ALLOW = 2;
        int DENY = 3;
        int ALLOW_ONCE = -1;
    }

    @Id
    private Long id;

    @Unique
    @Property(nameInDb = "pkg")
    private String packageName;

    @Type
    @Property(nameInDb = "type")
    private int type = Type.ASK;

    @Property(nameInDb = "notification_on_register")
    private boolean notificationOnRegister;

    @Property(nameInDb = "group_notifications_for_same_session")
    private boolean groupNotificationsForSameSession;

    @Property(nameInDb = "clear_all_notifications_of_session")
    private boolean clearAllNotificationsOfSession;

    @Property(nameInDb = "show_pass_through")
    private boolean showPassThrough;

    @IntDef({RegisteredType.NotRegistered, RegisteredType.Registered, RegisteredType.Unregistered})
    @Retention(SOURCE)
    @Target({ElementType.PARAMETER, ElementType.TYPE,
            ElementType.FIELD, ElementType.METHOD})
    public @interface RegisteredType {
        int NotRegistered = 0;
        int Registered = 1;
        int Unregistered = 2;
    }

    @RegisteredType
    private int registeredType = RegisteredType.NotRegistered;

    @Transient
    public boolean existServices = false;
    public String appName = "";
    @Transient
    public String appNamePinYin = "";
    @Transient
    public Date lastReceiveTime = new Date(0);

    @Generated(hash = 1327348616)
    public RegisteredApplication(Long id, String packageName, int type, boolean notificationOnRegister,
            boolean groupNotificationsForSameSession, boolean clearAllNotificationsOfSession, boolean showPassThrough,
            int registeredType, String appName) {
        this.id = id;
        this.packageName = packageName;
        this.type = type;
        this.notificationOnRegister = notificationOnRegister;
        this.groupNotificationsForSameSession = groupNotificationsForSameSession;
        this.clearAllNotificationsOfSession = clearAllNotificationsOfSession;
        this.showPassThrough = showPassThrough;
        this.registeredType = registeredType;
        this.appName = appName;
    }

    public RegisteredApplication() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }


    @NonNull
    public CharSequence getLabel (Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            return pm.getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES).loadLabel(pm);
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        } catch (Resources.NotFoundException e) {   // Not using multi-catch due to build error from greendao
            return packageName;
        }
    }

    @NonNull
    public Drawable getIcon (Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            return pm.getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES).loadIcon(pm);
        } catch (PackageManager.NameNotFoundException e) {
            return ContextCompat.getDrawable(context, android.R.mipmap.sym_def_app_icon);
        } catch (Resources.NotFoundException e) {   // Not using multi-catch due to build error from greendao
            return ContextCompat.getDrawable(context, android.R.mipmap.sym_def_app_icon);
        }
    }

    public int getUid (Context context) {
        try {
            return context.getPackageManager().getApplicationInfo(packageName,
                    PackageManager.GET_UNINSTALLED_PACKAGES)
                    .uid;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    public boolean isNotificationOnRegister() {
        return notificationOnRegister;
    }

    public void setNotificationOnRegister(boolean notificationOnRegister) {
        this.notificationOnRegister = notificationOnRegister;
    }

    public boolean getNotificationOnRegister() {
        return this.notificationOnRegister;
    }

    public boolean getGroupNotificationsForSameSession() {
        return this.groupNotificationsForSameSession;
    }

    public void setGroupNotificationsForSameSession(boolean groupNotificationsForSameSession) {
        this.groupNotificationsForSameSession = groupNotificationsForSameSession;
    }

    public boolean getClearAllNotificationsOfSession() {
        return this.clearAllNotificationsOfSession;
    }

    public void setClearAllNotificationsOfSession(boolean clearAllNotificationsOfSession) {
        this.clearAllNotificationsOfSession = clearAllNotificationsOfSession;
    }

    public boolean getShowPassThrough() {
        return this.showPassThrough;
    }

    public void setShowPassThrough(boolean showPassThrough) {
        this.showPassThrough = showPassThrough;
    }

    public @RegisteredType int getRegisteredType() {
        return this.registeredType;
    }

    public void setRegisteredType(@RegisteredType int registeredType) {
        this.registeredType = registeredType;
    }

    public String getAppName() {
        return this.appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
}
