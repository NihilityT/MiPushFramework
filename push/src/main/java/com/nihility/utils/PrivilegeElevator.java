package com.nihility.utils;

import com.topjohnwu.superuser.Shell;
import com.xiaomi.xmsf.BuildConfig;

public class PrivilegeElevator {

    static {
        // Set settings before the main shell can be created
        Shell.enableVerboseLogging = BuildConfig.DEBUG;
        Shell.setDefaultBuilder(Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        );
    }

    public static void tryToElevate() {
        Shell.getShell();
    }
}