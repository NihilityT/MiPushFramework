package top.trumeet.mipushframework.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.xiaomi.mipush.sdk.ManifestChecker;
import com.xiaomi.mipush.sdk.MessageHandleService;
import com.xiaomi.mipush.sdk.PushMessageHandler;
import com.xiaomi.push.service.PushConstants;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import top.trumeet.common.Constants;

@SuppressWarnings("unchecked")
public class MiPushManifestChecker {
    private static final String TAG = MiPushManifestChecker.class.getSimpleName();

    private final Context context;
    private final Class manifestChecker;
    private final Method checkServicesMethod;

    private MiPushManifestChecker(Class manifestChecker, Context context) throws NoSuchMethodException {
        this.manifestChecker = manifestChecker;
        this.context = context;
        this.checkServicesMethod = manifestChecker.getDeclaredMethod("checkServices", Context.class, PackageInfo.class);
        this.checkServicesMethod.setAccessible(true);
    }

    /**
     * Make sure com.xiaomi.xmsf was installed.
     */
    public static MiPushManifestChecker create(@NonNull Context context) throws PackageManager.NameNotFoundException, ClassNotFoundException, NoSuchMethodException {
        Class manifestChecker = context.createPackageContext(Constants.SERVICE_APP_NAME, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY)
                .getClassLoader().loadClass("com.xiaomi.mipush.sdk.ManifestChecker");
        return new MiPushManifestChecker(manifestChecker, context);
    }

    public boolean checkPermissions(PackageInfo packageInfo) {
        try {
            Method method = manifestChecker.getDeclaredMethod("checkPermissions", Context.class, PackageInfo.class);
            method.setAccessible(true);
            method.invoke(null, context, packageInfo);
            return true;
        } catch (Throwable e) {
            if (!isIllegalManifestException(e)) {
                Log.e(TAG, "checkPermissions", e);
            } else {
                Log.e(TAG, "checkPermissions: " + packageInfo.packageName + "," + ((InvocationTargetException) e).getCause().getMessage());
            }
            return false;
        }
    }

    public boolean checkReceivers(String packageName) {
        boolean result;
        try {
            Context appCtx = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
            Method method = manifestChecker.getDeclaredMethod("checkReceivers", Context.class);
            method.setAccessible(true);
            method.invoke(null, appCtx);
            result = true;
        } catch (Throwable e) {
            if (!isIllegalManifestException(e)) {
                Log.e(TAG, "checkReceivers", e);
            }
            result = false;
        }
        return result;
    }

    public boolean checkServices(PackageInfo pkgInfo) {
        try {
            Map<String, String> configServiceProcessMap = new HashMap<>();
            Map<String, ManifestChecker.ServiceCheckInfo> requiredServicesMap = new HashMap<>();
            requiredServicesMap.put(PushMessageHandler.class.getCanonicalName(), new ManifestChecker.ServiceCheckInfo(PushMessageHandler.class.getCanonicalName(), true, true, ""));
            requiredServicesMap.put(MessageHandleService.class.getCanonicalName(), new ManifestChecker.ServiceCheckInfo(MessageHandleService.class.getCanonicalName(), true, false, ""));
            if (pkgInfo.services != null) {
                for (ServiceInfo info : pkgInfo.services) {
                    if (!TextUtils.isEmpty(info.name) && requiredServicesMap.containsKey(info.name)) {
                        ManifestChecker.ServiceCheckInfo checkInfo = requiredServicesMap.remove(info.name);
                        boolean enabled = checkInfo.enabled;
                        boolean exported = checkInfo.exported;
                        String permission = checkInfo.permission;
                        if (enabled && !info.enabled) {
                            throw new ManifestChecker.IllegalManifestException(String.format("<service android:name=\"%1$s\" .../> in AndroidManifest had the wrong enabled attribute, which should be android:enabled=%2$b.", info.name, Boolean.valueOf(enabled)));
                        }
                        if (exported && !info.exported) {
                            throw new ManifestChecker.IllegalManifestException(String.format("<service android:name=\"%1$s\" .../> in AndroidManifest had the wrong exported attribute, which should be android:exported=%2$b.", info.name, Boolean.valueOf(exported)));
                        }
                        if (!TextUtils.isEmpty(permission) && !TextUtils.equals(permission, info.permission)) {
                            throw new ManifestChecker.IllegalManifestException(String.format("<service android:name=\"%1$s\" .../> in AndroidManifest had the wrong permission attribute, which should be android:permission=\"%2$s\".", info.name, permission));
                        }
                        configServiceProcessMap.put(info.name, info.processName);
                        if (requiredServicesMap.isEmpty()) {
                            break;
                        }
                    }
                }
            }
            if (!requiredServicesMap.isEmpty()) {
                throw new ManifestChecker.IllegalManifestException(String.format("<service android:name=\"%1$s\" .../> is missing or disabled in AndroidManifest.", requiredServicesMap.keySet().iterator().next()));
            }
            if (!TextUtils.equals(configServiceProcessMap.get(PushMessageHandler.class.getCanonicalName()), configServiceProcessMap.get(MessageHandleService.class.getCanonicalName()))) {
                throw new ManifestChecker.IllegalManifestException(String.format("\"%1$s\" and \"%2$s\" must be running in the same process.", PushMessageHandler.class.getCanonicalName(), MessageHandleService.class.getCanonicalName()));
            }
            if (configServiceProcessMap.containsKey(PushConstants.XM_SERVICE_CLASS_NAME_JAR) && configServiceProcessMap.containsKey(PushConstants.PUSH_SERVICE_CLASS_NAME_JAR) && !TextUtils.equals(configServiceProcessMap.get(PushConstants.XM_SERVICE_CLASS_NAME_JAR), configServiceProcessMap.get(PushConstants.PUSH_SERVICE_CLASS_NAME_JAR))) {
                throw new ManifestChecker.IllegalManifestException(String.format("\"%1$s\" and \"%2$s\" must be running in the same process.", PushConstants.XM_SERVICE_CLASS_NAME_JAR, PushConstants.PUSH_SERVICE_CLASS_NAME_JAR));
            }
            return true;
        } catch (Throwable e) {
            if (!isIllegalManifestException(e)) {
                Log.e(TAG, "checkServices", e);
            } else {
                Log.w(TAG, "checkServices: " + pkgInfo.packageName + "," + e.getMessage());
            }
            return false;
        }
    }

    private static boolean isIllegalManifestException(Throwable e) {
        if (e instanceof InvocationTargetException) {
            e = ((InvocationTargetException) e).getTargetException();
        }
        return e.getClass().getName().equals("com.xiaomi.mipush.sdk.ManifestChecker$IllegalManifestException");
    }
}
