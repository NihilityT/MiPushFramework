package com.xiaomi.push.service;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class PushHostManagerFactoryAspect {

    @Around("execution(* com.xiaomi.push.service.PushHostManagerFactory.GslbHttpGet.doGet(..)) && args(url)")
    public Object doGet(final ProceedingJoinPoint joinPoint, String url) throws Throwable {
        return joinPoint.proceed(new Object[]{url + "&countrycode=CN"});
    }
}
