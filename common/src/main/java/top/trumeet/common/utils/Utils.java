package top.trumeet.common.utils;

import static android.content.Context.APP_OPS_SERVICE;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.Html;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.Calendar;
import java.util.Date;

import top.trumeet.common.override.AppOpsManagerOverride;

public final class Utils {
    public static Context context;

    public static int myUid() {
        return Process.myUserHandle().hashCode();
    }

    public static Context getApplication() {
        return context;
    }

    public static PackageManager getPackageManager() {
        return context.getPackageManager();
    }

    public static boolean isAppOpsInstalled() {
        return isAppInstalled("rikka.appops");
    }

    public static boolean isAppInstalled(String packageName) {
        try {
            return getPackageManager().getPackageInfo(packageName, 0) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @ColorInt
    public static int getColorAttr(Context context, int attr) {
        TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
        @ColorInt int colorAccent = ta.getColor(0, 0);
        ta.recycle();
        return colorAccent;
    }

    public static Date getUTC(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int zoneOffset = cal.get(java.util.Calendar.ZONE_OFFSET);
        int dstOffset = cal.get(java.util.Calendar.DST_OFFSET);
        cal.add(java.util.Calendar.MILLISECOND, -(zoneOffset + dstOffset));
        return cal.getTime();
    }

    public static Date getUTC() {
        return getUTC(new Date());
    }

    public static CharSequence getString(@StringRes int id,
                                         @NonNull Context context,
                                         Object... formatArgs) {
        return toHtml(context.getString(id, formatArgs));
    }

    public static CharSequence toHtml(String str) {
        return Html.fromHtml(str);
    }

    public static boolean isUserApplication(ApplicationInfo applicationInfo) {
        return (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
    }

    public static int checkOp(Context context, int op) {
        return AppOpsManagerOverride.checkOpNoThrow(op, Process.myUid(),
                context.getPackageName(), (AppOpsManager) context.getSystemService(APP_OPS_SERVICE));
    }

    public static boolean isUserApplication(String pkg) {
        try {
            return isUserApplication(getApplication().getPackageManager().getApplicationInfo(pkg, PackageManager.GET_UNINSTALLED_PACKAGES));
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    public static void makeText(Context context, CharSequence usedString, int duration) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Toast.makeText(context, usedString, duration).show();
            } catch (Throwable ignored) {
                // TODO: It's a bad way to switch to main thread.
                // Ignored service death
            }
        });
    }

    public static String getRegSec(String packageName) {
        SharedPreferences secSp = getApplication().getSharedPreferences("pref_registered_pkg_names_sec", 0);
        String sec = secSp.getString(packageName, null);
        if (sec != null) {
            return sec;
        }
        secSp = getApplication().getSharedPreferences("mipush", 0);
        return secSp.getString(packageName, null);
    }

    public static void setRegSec(String pkgName, String regSec) {
        if (TextUtils.isEmpty(regSec)) {
            return;
        }
        SharedPreferences secSp = getApplication().getSharedPreferences("pref_registered_pkg_names_sec", 0);
        SharedPreferences.Editor secEditor = secSp.edit();
        secEditor.putString(pkgName, regSec);
        secEditor.commit();
    }

    public static Long getLastReceiveTime(String packageName) {
        SharedPreferences secSp = getApplication().getSharedPreferences("last_receive_time", 0);
        if (!secSp.contains(packageName)) {
            return null;
        }
        return secSp.getLong(packageName, 0);
    }

    public static void setLastReceiveTime(String pkgName, long time) {
        SharedPreferences secSp = getApplication().getSharedPreferences("last_receive_time", 0);
        SharedPreferences.Editor secEditor = secSp.edit();
        secEditor.putLong(pkgName, time);
        secEditor.commit();
    }
}
