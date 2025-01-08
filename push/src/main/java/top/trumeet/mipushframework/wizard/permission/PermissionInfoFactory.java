package top.trumeet.mipushframework.wizard.permission;

import android.content.Context;
import android.content.Intent;

import java.lang.reflect.Constructor;

public class PermissionInfoFactory {
    private static final String intentKey = PermissionInfo.class.getSimpleName();

    public static PermissionInfo create(String className, Context context) {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor(Context.class);
            return (PermissionInfo) constructor.newInstance(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Intent bindPermissionInfo(
            Intent intent, Class<? extends PermissionInfo> permissionInfoClass) {
        return intent.putExtra(intentKey, permissionInfoClass.getCanonicalName());
    }

    public static PermissionInfo createFrom(Intent intent, Context context) {
        String permissionInfoClass = intent.getStringExtra(intentKey);
        return create(permissionInfoClass, context);
    }

}
