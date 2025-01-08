package top.trumeet.mipushframework.wizard;

import android.content.Context;
import android.content.Intent;

public class PermissionInfoFactory {
    private static final String intentKey = PermissionInfo.class.getSimpleName();

    public static PermissionInfo create(String className, Context context) {
        if (UsageStatsPermissionInfo.class.getSimpleName().equals(className)) {
            return new UsageStatsPermissionInfo(context);
        }
        return null;
    }

    public static Intent bindPermissionInfo(
            Intent intent, Class<? extends PermissionInfo> permissionInfoClass) {
        return intent.putExtra(intentKey, permissionInfoClass.getSimpleName());
    }

    public static PermissionInfo createFrom(Intent intent, Context context) {
        String permissionInfoClass = intent.getStringExtra(intentKey);
        return create(permissionInfoClass, context);
    }

}
