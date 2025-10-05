package com.xiaomi.xmsf.push.utils;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.nihility.utils.Singleton;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmsf.utils.ConvertUtils;

public class ConfigValueConverter {
    private static final Logger logger = XLog.tag(ConfigValueConverter.class.getSimpleName()).build();

    public static ConfigValueConverter getInstance() {
        return Singleton.instance();
    }

    public <T> Object convert(Object root, String[] path, T value) {
        if (path.length == 1 && "pushAction".equals(path[0])) {
            try {
                XmPushActionContainer container = (XmPushActionContainer) root;
                return ConvertUtils.getResponseMessageBodyFromContainer(container,
                        RegSecUtils.getRegSec(container));
            } catch (Throwable e) {
                logger.e("parse pushAction failed", e);
                return null;
            }
        }
        return value;
    }
}
