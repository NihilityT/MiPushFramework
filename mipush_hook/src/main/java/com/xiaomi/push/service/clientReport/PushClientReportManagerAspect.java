package com.xiaomi.push.service.clientReport;

import com.nihility.Hooked;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class PushClientReportManagerAspect {
    @Around("execution(* com.xiaomi.push.service.clientReport.PushClientReportManager.collectData(..))")
    public void doNothingToAvoidTracking(final ProceedingJoinPoint joinPoint) {
        Hooked.mark(PushClientReportManager.class.getSimpleName());
    }
}
