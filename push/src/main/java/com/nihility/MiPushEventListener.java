package com.nihility;

import android.content.Intent;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.nihility.utils.Singleton;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmsf.utils.ConvertUtils;

public class MiPushEventListener {
    private static final String TAG = MiPushEventListener.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    public static MiPushEventListener instance() {
        return Singleton.instance();
    }

    public static void setInstance(MiPushEventListener listener) {
        Singleton.reset(listener);
    }

    public void receiveFromServer(XmPushActionContainer container) {
        logger.i("From Server     : " + ConvertUtils.toJson(container));
    }

    public void transferToApplication(XmPushActionContainer container) {
        logger.i("To   Application: " + ConvertUtils.toJson(container));
    }

    public void receiveFromApplication(Intent intent) {
        logger.i("From Application: " + ConvertUtils.toJson(intent));
    }

    public void transferToServer(Intent intent) {
        logger.i("To   Server     : " + ConvertUtils.toJson(intent));
    }

}
