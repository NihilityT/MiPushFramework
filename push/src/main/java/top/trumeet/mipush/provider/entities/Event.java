package top.trumeet.mipush.provider.entities;

import static com.xiaomi.push.service.MIPushEventProcessor.buildContainer;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Property;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import top.trumeet.common.utils.Utils;

/**
 * Created by Trumeet on 2017/8/26.
 * App event model
 * @author Trumeet
 */

@Entity
public class Event {
    public @Nullable XmPushActionContainer getContainer() {
        XmPushActionContainer container = getPayload() == null ?
                null : buildContainer(getPayload());
        return container;
    }

    @androidx.annotation.IntDef({Type.Notification,
            Type.Command, Type.AckMessage, Type.Registration,
            Type.MultiConnectionBroadcast, Type.MultiConnectionResult,
            Type.UnRegistration, Type.ReportFeedback, Type.SetConfig, Type.Subscription,
            Type.UnSubscription, Type.RegistrationResult})
    @Retention(SOURCE)
    @Target({ElementType.PARAMETER, ElementType.TYPE,
            ElementType.FIELD, ElementType.METHOD})
    public @interface Type {
        @Deprecated
        int RECEIVE_PUSH = 0;
        @Deprecated
        int RECEIVE_COMMAND = 1;
        @Deprecated
        int REGISTER = 2;

        // Same to com.xiaomi.xmpush.thrift.ActionType
        int Subscription = 3;
        int UnSubscription = 4;
        int SendMessage = RECEIVE_PUSH;
        int AckMessage = 6;
        int SetConfig = 7;
        int ReportFeedback = 8;
        int Notification = 9;
        int Command = 10;
        int MultiConnectionBroadcast = 11;
        int MultiConnectionResult = 12;

        // 和上面的重复（
        int Registration = REGISTER;
        int UnRegistration = 20;

        // Custom
        int RegistrationResult = 21;
    }

    @androidx.annotation.IntDef({ResultType.OK, ResultType.DENY_DISABLED, ResultType.DENY_USER})
    @Retention(SOURCE)
    @Target({ElementType.PARAMETER, ElementType.TYPE,
            ElementType.FIELD, ElementType.METHOD})
    public @interface ResultType {
        /**
         * Allowed
         */
        int OK = 0;

        /**
         * Deny because push is disabled by user
         */
        int DENY_DISABLED = 1;

        /**
         * User denied
         */
        int DENY_USER = 2;
    }

    /**
     * Id
     */
    @Id
    private Long id;

    /**
     * Package name
     */
    @Property(nameInDb = "pkg")
    @NotNull
    private String pkg;

    /**
     * Event type
     */
    @Property(nameInDb = "type")
    @Type
    @NotNull
    private int type;

    /**
     * Event date time (UTC)
     */
    @Property(nameInDb = "date")
    @NotNull
    private long date;

    /**
     * Operation result
     */
    @Property(nameInDb = "result")
    @ResultType
    private int result;

    @Property(nameInDb = "dev_info")
    private String info;

    @Property(nameInDb = "noti_title")
    private String notificationTitle;

    @Property(nameInDb = "noti_summary")
    private String notificationSummary;

    @Property(nameInDb = "payload")
    private byte[] payload;

    @Property(nameInDb = "reg_sec")
    private String regSec;

    @Generated(hash = 344677835)
    public Event() {
    }

    @Generated(hash = 715718956)
    public Event(Long id, @NotNull String pkg, int type, long date, int result, String info, String notificationTitle,
            String notificationSummary, byte[] payload, String regSec) {
        this.id = id;
        this.pkg = pkg;
        this.type = type;
        this.date = date;
        this.result = result;
        this.info = info;
        this.notificationTitle = notificationTitle;
        this.notificationSummary = notificationSummary;
        this.payload = payload;
        this.regSec = regSec;
    }

    public String getNotificationTitle() {
        return notificationTitle;
    }

    public void setNotificationTitle(String notificationTitle) {
        this.notificationTitle = notificationTitle;
    }

    public String getNotificationSummary() {
        return notificationSummary;
    }

    public void setNotificationSummary(String notificationSummary) {
        this.notificationSummary = notificationSummary;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPkg() {
        return this.pkg;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getDate() {
        return this.date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public int getResult() {
        return this.result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public byte[] getPayload() {
        return this.payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public String getRegSec() {
        if (TextUtils.isEmpty(this.regSec)) {
            return Utils.getRegSec(this.pkg);
        }
        return this.regSec;
    }

    public void setRegSec(String regSec) {
        this.regSec = regSec;
    }
}
