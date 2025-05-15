package com.android.settings.widget;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.topjohnwu.superuser.Shell;
import com.xiaomi.push.sdk.MyPushMessageHandler;
import com.xiaomi.push.service.PushConstants;
import com.xiaomi.push.service.XmPushActionOperator;
import com.xiaomi.xmpush.thrift.NotificationType;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmpush.thrift.XmPushActionNotification;
import com.xiaomi.xmpush.thrift.XmPushThriftSerializeUtils;

import top.trumeet.common.utils.Utils;

public class RegistrationHelper {
    Context context;
    String packageName;

    public RegistrationHelper(Context context, String packageName) {
        this.context = context;
        this.packageName = packageName;
    }

    public static void tryForceRegister(String packageName) {
        byte[] msgBytes = XmPushActionOperator.packToBytes(
                createForceRegisterMessage(packageName));
        Intent intent = new Intent(PushConstants.MIPUSH_ACTION_NEW_MESSAGE);
        intent.setPackage(packageName);
        intent.putExtra(PushConstants.MIPUSH_EXTRA_PAYLOAD, msgBytes);
        intent.putExtra(PushConstants.MESSAGE_RECEIVE_TIME, System.currentTimeMillis());

        Utils.getApplication().sendBroadcast(intent, null);
    }

    @NonNull
    public static XmPushActionContainer createForceRegisterMessage(String packageName) {
        String id = "fake_expired_" + packageName + "_" + System.currentTimeMillis();
        XmPushActionNotification regIdExpiredNotification = new XmPushActionNotification();
        regIdExpiredNotification.setType(NotificationType.RegIdExpired.value);
        regIdExpiredNotification.setId(id);
        PushMetaInfo metaInfo = new PushMetaInfo();
        metaInfo.setId(id);
        XmPushActionContainer regIdExpiredContainer = XmPushActionOperator.packToContainer(regIdExpiredNotification, packageName);
        regIdExpiredContainer.setMetaInfo(metaInfo);
        return regIdExpiredContainer;
    }

    public boolean removeMiPushXml() {
        Shell.Result result = Shell.cmd(String.format(
                "rm $(ls -1" +
                        " /data/user/0/%s/shared_prefs/mipush*.xml" +
                        " /data_mirror/data_ce/null/0/%s/shared_prefs/mipush*.xml" +
                        " 2> /dev/null)",
                packageName, packageName)
        ).exec();
        return result.isSuccess();
    }

    public void deleteRegistrationInfoAndRetryForceRegister() {
        removeMiPushXml();
        MyPushMessageHandler.launchApp(context, createForceRegisterMessage(packageName));
        tryForceRegister(packageName);
    }
}