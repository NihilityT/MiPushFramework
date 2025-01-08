package top.trumeet.mipushframework.wizard;

import android.content.Context;

public class PermissionInfoFactory {

    public static PermissionInfo create(String className, Context context) {
        if (UsageStatsPermissionInfo.class.getSimpleName().equals(className)) {
            return new UsageStatsPermissionInfo(context);
        }
        return null;
    }

}
