package com.nihility;

import android.content.Intent;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmsf.utils.ConvertUtils;

public class MiPushEventListener {
    private static final String TAG = MiPushEventListener.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    private static class LazyHolder {
        volatile static MiPushEventListener INSTANCE = new MiPushEventListener();
    }

    private static MiPushEventListener instance;

    public static MiPushEventListener instance() {
        return instance != null ? instance : LazyHolder.INSTANCE;
    }

    public static void setInstance(MiPushEventListener listener) {
        instance = listener;
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
