package top.trumeet.mipushframework.wizard;

import android.app.Activity;

import androidx.annotation.NonNull;

public interface PermissionInfo {
    @NonNull
    PermissionOperator getPermissionOperator();

    @NonNull
    String getPermissionTitle();

    @NonNull
    String getPermissionDescription();

    @NonNull
    Class<? extends Activity> nextPageClass();
}
