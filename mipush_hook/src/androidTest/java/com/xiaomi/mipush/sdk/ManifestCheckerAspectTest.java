package com.xiaomi.mipush.sdk;

import android.content.Context;
import android.content.pm.PackageInfo;

import androidx.test.core.app.ApplicationProvider;

import com.xiaomi.channel.commonutils.reflect.JavaCalls;
import com.xiaomi.push.service.PushConstants;

import org.junit.Test;

public class ManifestCheckerAspectTest {

    @Test
    public void ignoreWrongXmsfPermissionCheck() throws Throwable {
        shouldNotThrowWhenCheckXmsfPermission();
    }

    private static void shouldNotThrowWhenCheckXmsfPermission() throws Throwable {
        Context context = ApplicationProvider.getApplicationContext();
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                PushConstants.PUSH_SERVICE_PACKAGE_NAME, 4612);
        JavaCalls.callStaticMethodOrThrow(ManifestChecker.class, "checkServices", context, packageInfo);
    }
}