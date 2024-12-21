package com.xiaomi.push.service;

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
    private static final String TAG = XMPushServiceAspect.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    public RegisterRecorder() {
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

    static void toastErrorMessage(RuntimeException e) {
        Utils.makeText(XMPushServiceAspect.xmPushService, XMPushServiceAspect.xmPushService.getString(R.string.common_err, e.getMessage()), Toast.LENGTH_LONG);
    }

    static void saveRegisterAppRecord(String pkg) {
        EventDb.insertEvent(Event.ResultType.OK,
                new RegistrationType(null, pkg, null)
        );
    }

    static boolean isRegisterAppRequest(Intent intent) {
        return intent != null && PushConstants.MIPUSH_ACTION_REGISTER_APP.equals(intent.getAction());
    }

    void showRegisterToastIfUserAllow(RegisteredApplication application) {
        if (canShowRegisterNotification(application)) {
            showRegisterNotification(application);
        } else {
            Log.e("XMPushService Bridge", "Notification disabled");
        }
    }

    static void showRegisterNotification(RegisteredApplication application) {
        CharSequence appName = ApplicationNameCache.getInstance().getAppName(XMPushServiceAspect.xmPushService, application.getPackageName());
        CharSequence usedString = XMPushServiceAspect.xmPushService.getString(R.string.notification_registerAllowed, appName);
        Utils.makeText(XMPushServiceAspect.xmPushService, usedString, Toast.LENGTH_SHORT);
    }

    static boolean canShowRegisterNotification(RegisteredApplication application) {
        boolean notificationOnRegister = ConfigCenter.getInstance().isNotificationOnRegister(XMPushServiceAspect.xmPushService);
        notificationOnRegister = notificationOnRegister && application.isNotificationOnRegister();
        return notificationOnRegister;
    }
}