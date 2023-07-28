package com.xiaomi.xmsf.push.service;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.xmsf.R;
import com.xiaomi.xmsf.push.control.PushControllerUtils;
import com.xiaomi.xmsf.push.utils.Configurations;
import com.xiaomi.xmsf.push.utils.IconConfigurations;
import com.xiaomi.xmsf.utils.ConfigCenter;
import com.xiaomi.xmsf.utils.ConvertUtils;

import top.trumeet.common.Constants;
import top.trumeet.common.utils.Utils;

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
        } catch (RuntimeException e) {
            logger.e("XMPushService::onHandleIntent: ", e);
            Utils.makeText(this, getString(R.string.common_err, e.getMessage()), Toast.LENGTH_LONG);
        }
    }

    private void forwardToPushServiceMain(Intent intent) {
        Intent intent2 = new Intent();
        intent2.setComponent(new ComponentName(this, com.xiaomi.push.service.XMPushService.class));
        intent2.setAction(intent.getAction());
        intent2.putExtras(intent);
        ContextCompat.startForegroundService(this, intent2);
        logger.d("forward intent " + ConvertUtils.toJson(intent));
    }

}
