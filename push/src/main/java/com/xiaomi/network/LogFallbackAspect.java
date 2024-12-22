package com.xiaomi.network;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class LogFallbackAspect {
    private static final String TAG = LogFallbackAspect.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    @Before("execution(* com.xiaomi.network.Fallback.getHosts(..)) && target(fallback) && args(usePort)")
    public void logFallback(final JoinPoint joinPoint, Fallback fallback, boolean usePort) {
        logger.d(joinPoint.getSignature());
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();
        logger.d("Fallback " + gson.toJsonTree(fallback));
    }

}
