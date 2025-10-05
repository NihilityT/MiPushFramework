package com.xiaomi.network;


import android.text.TextUtils;

import com.nihility.Configurations;
import com.nihility.Dependencies;
import com.xiaomi.smack.ConnectionConfiguration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.ArrayList;

@Aspect
public class FallbackAspect {

    @Around("execution(* com.xiaomi.network.Fallback.getHosts(..)) && target(fallback) && args(usePort)")
    public Object useUserDefinedXmppServerHostFirst(final ProceedingJoinPoint joinPoint, Fallback fallback, boolean usePort) throws Throwable {
        ArrayList<String> hosts = (ArrayList<String>) joinPoint.proceed();

        if (TextUtils.equals(fallback.host, ConnectionConfiguration.getXmppServerHost())) {
            Configurations configurations = Dependencies.instance().configuration();
            String userHost = configurations.getXMPPServer();
            if (!TextUtils.isEmpty(userHost)) {
                hosts.add(0, userHost);
            }
        }
        return hosts;
    }

}
