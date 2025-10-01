package com.xiaomi.xmsf;

import android.widget.Toast;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.elvishew.xlog.flattener.ClassicFlattener;
import com.elvishew.xlog.printer.file.FilePrinter;
import com.elvishew.xlog.printer.file.backup.NeverBackupStrategy;
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy;
import com.elvishew.xlog.printer.file.naming.DateFileNameGenerator;
import com.xiaomi.xmsf.utils.LogUtils;

import top.trumeet.common.utils.Utils;

public class CrashHandler {
    private static Thread.UncaughtExceptionHandler defaultHandler;

    static {
        initDefaultHandler();
    }

    public static void installCrashLogger() {
        final String TAG = CrashHandler.class.getSimpleName();
        final Logger logger = XLog.tag(TAG).build();
        final Logger crashLogger = XLog.tag(TAG).printers(createCrashPrinter()).build();

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
            crashLogger.e("Mi Push Crash", e);
        });
    }

    private static FilePrinter createCrashPrinter() {
        int _7DaysInMillis = 7 * 24 * 60 * 60 * 1000;
        return new FilePrinter.Builder(LogUtils.getLogFolder(Utils.getApplication()))
                .fileNameGenerator(new DateFileNameGenerator() {
                    @Override
                    public String generateFileName(int logLevel, long timestamp) {
                        return "Crash_" + super.generateFileName(logLevel, timestamp);
                    }
                })
                .backupStrategy(new NeverBackupStrategy())
                .cleanStrategy(new FileLastModifiedCleanStrategy(_7DaysInMillis))
                .flattener(new ClassicFlattener())
                .build();
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
