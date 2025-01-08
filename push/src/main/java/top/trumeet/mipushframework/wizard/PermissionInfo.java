package top.trumeet.mipushframework.wizard;

import android.content.Intent;

import androidx.annotation.NonNull;

public interface PermissionInfo {
    @NonNull
    PermissionOperator getPermissionOperator();

    @NonNull
    String getPermissionTitle();

    @NonNull
    String getPermissionDescription();

    @NonNull
    Intent nextPageIntent();
}
