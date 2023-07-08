package com.xiaomi.xmsf.push.utils;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import top.trumeet.mipushframework.event.EventItemBinder;

public class ConfigValueConverter {
    private static final Logger logger = XLog.tag(ConfigValueConverter.class.getSimpleName()).build();

    private static class LazyHolder {
        volatile static ConfigValueConverter INSTANCE = new ConfigValueConverter();
    }

    public static ConfigValueConverter getInstance() {
        return LazyHolder.INSTANCE;
    }

    public <T> Object convert(Object root, String[] path, T value) {
        if (path.length == 1 && "pushAction".equals(path[0])) {
            try {
                XmPushActionContainer container = (XmPushActionContainer) root;
                return EventItemBinder.getResponseMessageBodyFromContainer(container,
                        Utils.getRegSec(container));
            } catch (Throwable e) {
                logger.e("parse pushAction failed", e);
                return null;
            }
        }
        return value;
    }
}
