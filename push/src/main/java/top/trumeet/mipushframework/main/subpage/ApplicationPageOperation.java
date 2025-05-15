package top.trumeet.mipushframework.main.subpage;

import static top.trumeet.mipush.provider.db.RegisteredApplicationDb.registerApplication;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.github.promeg.pinyinhelper.Pinyin;
import com.xiaomi.xmsf.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import top.trumeet.common.cache.ApplicationNameCache;
import top.trumeet.common.utils.ElapsedTimer;
import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.db.EventDb;
import top.trumeet.mipush.provider.db.RegisteredApplicationDb;
import top.trumeet.mipush.provider.entities.RegisteredApplication;
import top.trumeet.mipushframework.utils.MiPushManifestChecker;

public class ApplicationPageOperation {
    private static final Logger logger = XLog.tag(ApplicationPageOperation.class.getSimpleName()).build();
    
    static public MiPushApplications getMiPushApplications() {
        MiPushApplications miPushApplications = new MiPushApplications();
        logger.d("[loadApp] start load app list");
        ElapsedTimer timer = new ElapsedTimer();
        Map<String, RegisteredApplication> registeredPkgs = getRegisteredApplicationMap(miPushApplications);
        logger.d("[loadApp] get registeredPkgs ms: %d", timer.restart());

        final List<PackageInfo> packageInfos = getPackagesOnDevice();
        miPushApplications.totalPkg = packageInfos.size();
        logger.d("[loadApp] get package info ms: %d", timer.restart());

        removePackagesThatNotSupportMiPushServices(packageInfos, registeredPkgs);
        logger.d("[loadApp] filter not service package ms: %d", timer.restart());

        List<RegisteredApplication> res = convertToRegisteredApplicationList(packageInfos, registeredPkgs);
        miPushApplications.res = res;
        logger.d("[loadApp] convert to application list ms: %d", timer.restart());

        addApplicationNameIfMissing(res);
        logger.d("[loadApp] query name ms: %d", timer.restart());

        addApplicationPinYinName(res);
        logger.d("[loadApp] query pinyin ms: %d", timer.restart());

        addLastReceiveTimeInfo(res);
        logger.d("[loadApp] query lastReceiveTime ms: %d", timer.restart());
        return miPushApplications;
    }

    public static void addLastReceiveTimeInfo(List<RegisteredApplication> res) {
        for (RegisteredApplication application : res) {
            application.lastReceiveTime = new Date(EventDb.getLastReceiveTime(application.getPackageName()));
        }
    }

    public static void addApplicationPinYinName(List<RegisteredApplication> res) {
        for (RegisteredApplication application : res) {
            application.appNamePinYin = Pinyin.toPinyin(application.appName, "");
        }
    }

    public static void addApplicationNameIfMissing(List<RegisteredApplication> res) {
        for (RegisteredApplication application : res) {
            if (!TextUtils.isEmpty(application.appName)) {
                continue;
            }
            application.appName = ApplicationNameCache.getInstance()
                    .getAppName(Utils.getApplication(), application.getPackageName()).toString();
        }
    }

    public static @NonNull List<RegisteredApplication> convertToRegisteredApplicationList(List<PackageInfo> packageInfos, Map<String, RegisteredApplication> registeredPkgs) {
        MiPushManifestChecker checker = getMiPushManifestChecker();
        List<RegisteredApplication> res = new ArrayList<RegisteredApplication>();
        for (PackageInfo info : packageInfos) {
            RegisteredApplication application = getRegisteredApplication(info, registeredPkgs, checker);
            res.add(application);
        }
        return res;
    }

    public static @NonNull RegisteredApplication getRegisteredApplication(PackageInfo info, Map<String, RegisteredApplication> registeredPkgs, MiPushManifestChecker checker) {
        String currentAppPkgName = info.packageName;
        RegisteredApplication application;
        if (registeredPkgs.containsKey(currentAppPkgName)) {
            application = registeredPkgs.get(currentAppPkgName);
        } else {
            application = registerApplication(currentAppPkgName);
        }
        application.existServices = hasMiPushServices(checker, info);
        return application;
    }

    public static void removePackagesThatNotSupportMiPushServices(List<PackageInfo> packageInfos, Map<String, RegisteredApplication> registeredPkgs) {
        MiPushManifestChecker checker = getMiPushManifestChecker();
        for (final Iterator<PackageInfo> iterator = packageInfos.iterator(); iterator.hasNext(); ) {
            PackageInfo info = iterator.next();
            if (!shouldShowInList(info, registeredPkgs, checker)) {
                iterator.remove();
            }
        }
    }

    public static boolean shouldShowInList(PackageInfo info, Map<String, RegisteredApplication> registeredPkgs, MiPushManifestChecker checker) {
        return isApplicationInstalled(info) &&
                (isPackageStoredInDB(registeredPkgs, info) || hasMiPushServices(checker, info));
    }

    public static boolean hasMiPushServices(MiPushManifestChecker checker, PackageInfo info) {
        return checker != null && checker.checkServices(info);
    }

    public static boolean isPackageStoredInDB(Map<String, RegisteredApplication> registeredPkgs, PackageInfo info) {
        return registeredPkgs.containsKey(info.applicationInfo.packageName);
    }

    public static boolean isApplicationInstalled(PackageInfo info) {
        return (info.applicationInfo.flags & ApplicationInfo.FLAG_INSTALLED) != 0;
    }

    public static @NonNull List<PackageInfo> getPackagesOnDevice() {
        return Utils.getApplication().getPackageManager().getInstalledPackages(
                PackageManager.GET_DISABLED_COMPONENTS |
                        PackageManager.GET_SERVICES |
                        PackageManager.GET_RECEIVERS);
    }

    public static @Nullable MiPushManifestChecker getMiPushManifestChecker() {
        MiPushManifestChecker checker = null;
        try {
            checker = MiPushManifestChecker.create(Utils.getApplication());
        } catch (PackageManager.NameNotFoundException | ClassNotFoundException |
                 NoSuchMethodException e) {
            logger.e("Create mi push checker", e);
        }
        return checker;
    }

    public static Map<String, RegisteredApplication> getRegisteredApplicationMap(MiPushApplications miPushApplications) {
        Map<String, RegisteredApplication> registeredPkgs = miPushApplications.registeredPkgs;
        for (RegisteredApplication application : RegisteredApplicationDb.getList(null)) {
            registeredPkgs.put(application.getPackageName(), application);
        }
        return registeredPkgs;
    }

    static void removeApplicationsThatQueryNotMatched(MiPushApplications miPushApplications, String query) {
        for (final Iterator<RegisteredApplication> iterator = miPushApplications.res.iterator(); iterator.hasNext(); ) {
            RegisteredApplication info = iterator.next();
            if (!isQueryMatched(info, query)) {
                iterator.remove();
            }
        }
    }

    static void sortApplicationsForDisplay(MiPushApplications miPushApplications) {
        Collections.sort(miPushApplications.res, (o1, o2) -> {
            if (o1.getId() == null && o2.getId() == null ||
                    o1.getRegisteredType() == RegisteredApplication.RegisteredType.NotRegistered &&
                            o2.getRegisteredType() == RegisteredApplication.RegisteredType.NotRegistered) {
                return o1.appNamePinYin.compareTo(o2.appNamePinYin);
            }

            if (o1.getId() == null) {
                return 1;
            }

            if (o2.getId() == null) {
                return -1;
            }

            if (o1.getRegisteredType() == RegisteredApplication.RegisteredType.NotRegistered) {
                return 1;
            }

            if (o2.getRegisteredType() == RegisteredApplication.RegisteredType.NotRegistered) {
                return -1;
            }

            if (o1.getRegisteredType() != o2.getRegisteredType()) {
                return o1.getRegisteredType() - o2.getRegisteredType();
            }
            int cmp = o2.lastReceiveTime.compareTo(o1.lastReceiveTime);
            if (cmp != 0) {
                return cmp;
            }
            return o1.appNamePinYin.compareTo(o2.appNamePinYin);
        });
    }

    private static boolean isQueryMatched(RegisteredApplication info, String query) {
        return info.getPackageName().toLowerCase().contains(query) ||
                info.appName.toLowerCase().contains(query) ||
                info.appNamePinYin.contains(query);
    }

    static MiPushApplications getMiPushApplicationsThatQueryMatched(String query) {
        ElapsedTimer totalTimer = new ElapsedTimer();
        MiPushApplications miPushApplications = getMiPushApplications();

        ElapsedTimer timer = new ElapsedTimer();
        removeApplicationsThatQueryNotMatched(miPushApplications, query);
        logger.d("[loadApp] filter app search ms: %d", timer.restart());

        sortApplicationsForDisplay(miPushApplications);
        logger.d("[loadApp] sort application list will show ms: %d", timer.restart());
        logger.d("[loadApp] end load app list ms: %d", totalTimer.elapsed());
        return miPushApplications;
    }

    static void updateRegisteredApplicationDb(Context context, List<RegisteredApplication> list) {
        ElapsedTimer totalTimer = new ElapsedTimer();
        ElapsedTimer timer = new ElapsedTimer();
        EventDb.RegistrationInfo registrationInfo = EventDb.queryRegistered();
        logger.d("[updateApp] get registeredPkgsFromEvents ms: %d", timer.restart());

        for (RegisteredApplication application : list) {
            String pkg = application.getPackageName();
            application.appName = ApplicationNameCache.getInstance()
                    .getAppName(context, pkg).toString();
            if (registrationInfo.registered.contains(pkg)) {
                application.setRegisteredType(RegisteredApplication.RegisteredType.Registered);
            } else if (registrationInfo.unregistered.contains(pkg)) {
                application.setRegisteredType(RegisteredApplication.RegisteredType.Unregistered);
            } else {
                application.setRegisteredType(RegisteredApplication.RegisteredType.NotRegistered);
            }
            RegisteredApplicationDb.update(application);
        }
        logger.d("[updateApp] update app ms: %d", timer.restart());
        logger.d("[updateApp] updated ms: %d", totalTimer.elapsed());
    }

    static @NonNull String getNotSupportHint(Context context, int notUseMiPushCount) {
        return context.getString(R.string.footer_app_ignored_not_registered, Integer.toString(notUseMiPushCount));
    }

    public static class MiPushApplications {
        public Map<String, RegisteredApplication> registeredPkgs = new HashMap<String, RegisteredApplication>();
        public List<RegisteredApplication> res = new ArrayList<RegisteredApplication>();
        public int totalPkg = 0;
    }
}