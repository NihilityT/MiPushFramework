package com.xiaomi.network;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.aspectj.lang.JoinPoint;

public class LogFallbackAspect {
    private static final String TAG = LogFallbackAspect.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    public void logFallback(final JoinPoint joinPoint, Fallback fallback, boolean usePort) {
        logger.d(joinPoint.getSignature());
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();
        logger.d("Fallback " + gson.toJsonTree(fallback));
    }

}
