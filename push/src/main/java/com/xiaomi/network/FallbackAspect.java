package com.xiaomi.network;

import android.content.Context;
import android.text.TextUtils;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xiaomi.channel.commonutils.network.Network;
import com.xiaomi.smack.ConnectionConfiguration;
import com.xiaomi.xmsf.utils.ConfigCenter;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.net.URL;
import java.util.ArrayList;

import top.trumeet.common.utils.Utils;

@Aspect
public class FallbackAspect {
    private static final String TAG = FallbackAspect.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    @Around("execution(* com.xiaomi.network.Fallback.getHosts(..)) && target(fallback) && args(usePort)")
    public Object getHosts(final ProceedingJoinPoint joinPoint, Fallback fallback, boolean usePort) throws Throwable {
        logger.d(joinPoint.getSignature());
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();
        logger.d("Fallback " + gson.toJsonTree(fallback));
        ArrayList<String> hosts = (ArrayList<String>) joinPoint.proceed();

        if (TextUtils.equals(fallback.host, ConnectionConfiguration.XMPP_SERVER_HOST_P)) {
            String userHost = ConfigCenter.getInstance().getXMPPServer(Utils.getApplication());
            if (!TextUtils.isEmpty(userHost)) {
                hosts.add(0, userHost);
            }
        }
        return hosts;
    }

}
