package com.xiaomi.xmsf;

import android.widget.Toast;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import top.trumeet.common.utils.Utils;

public class CrashHandler {
    private static Thread.UncaughtExceptionHandler defaultHandler;

    static {
        initDefaultHandler();
    }

    public static void installCrashLogger() {
        final String TAG = CrashHandler.class.getSimpleName();
        final Logger logger = XLog.tag(TAG).build();

        install((t, e) -> {
            StringBuilder crashInfo = new StringBuilder();
            crashInfo.append("Mi Push Crash:\n");
            crashInfo.append(e);
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (int i = 0; i < 3 && i < stackTrace.length; ++i) {
                crashInfo.append("\n    ");
                crashInfo.append(stackTrace[i]);
            }
            Utils.makeText(crashInfo, Toast.LENGTH_LONG);
            logger.e("Mi Push Crash", e);
        });
    }

    public static void install(Thread.UncaughtExceptionHandler handler) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            handler.uncaughtException(t, e);
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(t, e);
            }
        });
    }

    public static void uninstall() {
        Thread.setDefaultUncaughtExceptionHandler(defaultHandler);
    }

    public static void initDefaultHandler() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }
}
