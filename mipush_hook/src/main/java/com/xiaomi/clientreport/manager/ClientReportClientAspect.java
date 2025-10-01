package com.xiaomi.clientreport.manager;

import com.nihility.Hooked;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class ClientReportClientAspect {

    @Around("execution(* com.xiaomi.clientreport.manager.ClientReportClient.init(..))")
    public void doNothingToAvoidTracking(final ProceedingJoinPoint joinPoint) {
        Hooked.mark(ClientReportClient.class.getSimpleName());
    }
}
