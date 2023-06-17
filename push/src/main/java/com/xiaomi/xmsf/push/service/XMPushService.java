package com.xiaomi.xmsf.push.service;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.push.service.PushConstants;
import com.xiaomi.push.service.PushServiceMain;
import com.xiaomi.xmsf.R;
import com.xiaomi.xmsf.push.control.PushControllerUtils;
import com.xiaomi.xmsf.push.utils.Configurations;
import com.xiaomi.xmsf.push.utils.IconConfigurations;
import com.xiaomi.xmsf.utils.ConfigCenter;

import top.trumeet.common.Constants;
import top.trumeet.common.cache.ApplicationNameCache;
import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.db.EventDb;
import top.trumeet.mipush.provider.db.RegisteredApplicationDb;
import top.trumeet.mipush.provider.event.Event;
import top.trumeet.mipush.provider.event.type.RegistrationType;
import top.trumeet.mipush.provider.register.RegisteredApplication;

public class XMPushService extends IntentService {
    private static final String TAG = "XMPushService Bridge";
    private final Logger logger = XLog.tag(TAG).build();

    public XMPushService() {
        super("XMPushService Bridge");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (Constants.CONFIGURATIONS_UPDATE_ACTION.equals(intent.getAction())) {
            if (!PushControllerUtils.isAppMainProc(this)) {
                boolean success = Configurations.getInstance().init(this,
                        ConfigCenter.getInstance().getConfigurationDirectory(this)) &&
                        IconConfigurations.getInstance().init(this,
                                ConfigCenter.getInstance().getConfigurationDirectory(this));
            }
            return;
        }

        try {
            forwardToPushServiceMain(intent);

            String pkg = intent.getStringExtra(Constants.EXTRA_MI_PUSH_PACKAGE);
            if (pkg == null) {
                logger.e("Package name is NULL!");
                return;
            }

            RegisteredApplication application = RegisteredApplicationDb
                    .registerApplication(pkg, true);

            if (application == null) {
                return;
            }

            if (!PushConstants.MIPUSH_ACTION_REGISTER_APP.equals(intent.getAction())) {
                return;
            }
            logger.d("onHandleIntent -> A application want to register push");
            showRegisterToastIfExistsConfiguration(application);
            EventDb.insertEvent(Event.ResultType.OK,
                    new RegistrationType(null, pkg, null)
            );
        } catch (RuntimeException e) {
            logger.e("XMPushService::onHandleIntent: ", e);
            Utils.makeText(this, getString(R.string.common_err, e.getMessage()), Toast.LENGTH_LONG);
        }
    }

    private void showRegisterToastIfExistsConfiguration(RegisteredApplication application) {
        String pkg = application.getPackageName();
        boolean notificationOnRegister = ConfigCenter.getInstance().isNotificationOnRegister(this);
        notificationOnRegister = notificationOnRegister && application.isNotificationOnRegister();
        if (notificationOnRegister) {
            CharSequence appName = ApplicationNameCache.getInstance().getAppName(this, pkg);
            CharSequence usedString = getString(R.string.notification_registerAllowed, appName);
            Utils.makeText(this, usedString, Toast.LENGTH_SHORT);
        } else {
            Log.e("XMPushService Bridge", "Notification disabled");
        }
    }

    private void forwardToPushServiceMain(Intent intent) {
        Intent intent2 = new Intent();
        intent2.setComponent(new ComponentName(this, PushServiceMain.class));
        intent2.setAction(intent.getAction());
        intent2.putExtras(intent);
        ContextCompat.startForegroundService(this, intent2);
    }

}
