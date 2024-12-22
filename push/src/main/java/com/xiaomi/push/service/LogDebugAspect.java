package com.xiaomi.push.service;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;
import com.xiaomi.xmsf.utils.ConfigCenter;
import com.xiaomi.xmsf.utils.ConvertUtils;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.Collections;

@Aspect
public class LogDebugAspect {
    private static final String TAG = LogDebugAspect.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();
    private static final ThreadLocal<Integer> level = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };


    @Around("   !call(* *..DebugAspect.*(..))" +
            "&& !call(* com.xiaomi.channel.commonutils.reflect..*(..))" +
            "&& !call(* com.xiaomi.channel.commonutils.logger..*(..))" +
            "&& !call(* com.xiaomi.push.service.JobScheduler..*(..))" +
            "&& !call(* com.xiaomi.push.service.RC4Cryption..*(..))" +
            "&& (" +
            "   call(* com.xiaomi.mipush..*(..))" +
            "|| call(* com.xiaomi.push..*(..))" +
            "|| call(* com.xiaomi.channel..*(..))" +
            "|| execution(* com.xiaomi.push.service.XMPushService$2.handleMessage(..))" +
            ")")
    public Object logger(final ProceedingJoinPoint joinPoint) throws Throwable {
        if (!ConfigCenter.getInstance().isDebugMode()) {
            return joinPoint.proceed();
        }

        int curLevel = level.get();
        String prefix =String.join("", Collections.nCopies(curLevel, "|\t"));
        if (joinPoint.getThis() != null) {
            prefix += joinPoint.getThis().getClass().getSimpleName() + " " + joinPoint.toLongString();
        } else {
            prefix += joinPoint.toLongString();
        }
        logger.d(prefix);
        level.set(curLevel + 1);
        Object ret;
        try {
            ret = joinPoint.proceed();
        } finally {
            level.set(curLevel);
        }
        if (ret instanceof XmPushActionContainer) {
            logger.d(prefix + " -> [" + ConvertUtils.toJson((XmPushActionContainer) ret) + "]");
        } else {
            logger.d(prefix + " -> [" + ret + "]");
        }
        return ret;
    }

}
