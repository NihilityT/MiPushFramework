package com.xiaomi.push.service;

import android.net.Uri;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class PushHostManagerFactoryAspect {

    @Around("execution(* com.xiaomi.push.service.PushHostManagerFactory.GslbHttpGet.doGet(..)) && args(url)")
    public Object addCountryCodeToConfigurationUrl(final ProceedingJoinPoint joinPoint, String url) throws Throwable {
        url = url.replaceFirst("&countrycode=[^&]+", "");
        url = url.replaceFirst("\\?countrycode=[^&]+", "?");
        Uri.Builder uri = Uri.parse(url).buildUpon();
        uri.appendQueryParameter("countrycode", "CN");
        return joinPoint.proceed(new Object[]{uri.toString()});
    }
}
