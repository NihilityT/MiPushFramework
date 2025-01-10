package com.xiaomi.push.service;

import androidx.annotation.NonNull;

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
    private static final ThreadLocal<Integer> indentLevel = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };


    @Around("(execution(* com.xiaomi.push.service.XMPushService*.*(..))" +
            " || execution(* com.xiaomi.push.service.PacketSync*.*(..))" +
            " || execution(* com.xiaomi.push.service.ClientEventDispatcher*.*(..))" +
            " || execution(* com.xiaomi.push.service.MIPushEventProcessor*.*(..))" +
            " || execution(* com.xiaomi.push.service.MIPushNotificationHelper*.*(..))" +
            " || execution(* com.xiaomi.push.service.NotificationManagerHelper*.*(..))" +
            ") && !within(is(FinalType))")
    public Object logger(final ProceedingJoinPoint joinPoint) throws Throwable {
        if (!ConfigCenter.getInstance().isDebugMode()) {
            return joinPoint.proceed();
        }

        String prefix = getPrefix(joinPoint, indentLevel.get());
        logger.d(prefix);

        increaseIndent();
        Object ret;
        try {
            ret = joinPoint.proceed();
        } catch (Throwable e) {
            logThrowable(e, prefix);
            throw e;
        } finally {
            decreaseIndent();
        }

        logResult(ret, prefix);
        return ret;
    }

    private static void logThrowable(Throwable e, String prefix) {
        logger.d(prefix + " -> [" + e + "]");
    }

    private static void logResult(Object ret, String prefix) {
        if (ret instanceof XmPushActionContainer) {
            logger.d(prefix + " -> [" + ConvertUtils.toJson((XmPushActionContainer) ret) + "]");
        } else {
            logger.d(prefix + " -> [" + ret + "]");
        }
    }

    private static void decreaseIndent() {
        indentLevel.set(indentLevel.get() - 1);
    }

    private static void increaseIndent() {
        indentLevel.set(indentLevel.get() + 1);
    }

    @NonNull
    private static String getPrefix(ProceedingJoinPoint joinPoint, int curLevel) {
        String prefix = String.join("", Collections.nCopies(curLevel, "|\t"));
        if (joinPoint.getThis() != null) {
            prefix += joinPoint.getThis().getClass().getSimpleName() + " " + joinPoint.toLongString();
        } else {
            prefix += joinPoint.toLongString();
        }
        return prefix;
    }

}
