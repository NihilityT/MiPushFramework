package com.xiaomi.push.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.xmsf.R;
import com.xiaomi.xmsf.utils.ConfigCenter;

import top.trumeet.common.Constants;
import top.trumeet.common.cache.ApplicationNameCache;
import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.db.EventDb;
import top.trumeet.mipush.provider.db.RegisteredApplicationDb;
import top.trumeet.mipush.provider.event.Event;
import top.trumeet.mipush.provider.event.type.RegistrationType;
import top.trumeet.mipush.provider.register.RegisteredApplication;

public class RegisterRecorder {
    private final String TAG = RegisterRecorder.class.getSimpleName();
    private final Logger logger = XLog.tag(TAG).build();
    private final Context context;

    public RegisterRecorder(Context context) {
        this.context = context;
    }

    void recordRegisterRequest(Intent intent) {
        try {
            if (!isRegisterAppRequest(intent)) {
                return;
            }

            String pkg = intent.getStringExtra(Constants.EXTRA_MI_PUSH_PACKAGE);
            if (pkg == null) {
                logger.e("Package name is NULL!");
                return;
            }

            logger.d("onHandleIntent -> A application want to register push");
            showRegisterToastIfUserAllow(
                    RegisteredApplicationDb.registerApplication(pkg, true));
            saveRegisterAppRecord(pkg);
        } catch (RuntimeException e) {
            logger.e("XMPushService::onHandleIntent: ", e);
            toastErrorMessage(e);
        }
    }

    void toastErrorMessage(RuntimeException e) {
        Utils.makeText(context, context.getString(R.string.common_err, e.getMessage()), Toast.LENGTH_LONG);
    }

    void saveRegisterAppRecord(String pkg) {
        EventDb.insertEvent(Event.ResultType.OK,
                new RegistrationType(null, pkg, null)
        );
    }

    boolean isRegisterAppRequest(Intent intent) {
        return intent != null && PushConstants.MIPUSH_ACTION_REGISTER_APP.equals(intent.getAction());
    }

    void showRegisterToastIfUserAllow(RegisteredApplication application) {
        if (canShowRegisterNotification(application)) {
            showRegisterNotification(application);
        } else {
            Log.e("XMPushService Bridge", "Notification disabled");
        }
    }

    void showRegisterNotification(RegisteredApplication application) {
        CharSequence appName = ApplicationNameCache.getInstance().getAppName(context, application.getPackageName());
        CharSequence usedString = context.getString(R.string.notification_registerAllowed, appName);
        Utils.makeText(context, usedString, Toast.LENGTH_SHORT);
    }

    boolean canShowRegisterNotification(RegisteredApplication application) {
        boolean notificationOnRegister = ConfigCenter.getInstance().isNotificationOnRegister(context);
        notificationOnRegister = notificationOnRegister && application.isNotificationOnRegister();
        return notificationOnRegister;
    }
}