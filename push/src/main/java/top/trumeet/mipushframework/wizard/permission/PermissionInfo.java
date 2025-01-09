package top.trumeet.mipushframework.wizard.permission;

import androidx.annotation.NonNull;

public interface PermissionInfo {
    @NonNull
    PermissionOperator getPermissionOperator();

    @NonNull
    String getPermissionTitle();

    @NonNull
    String getPermissionDescription();

}
