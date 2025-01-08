package top.trumeet.mipushframework.wizard;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UsageStatsPermissionActivity extends RequestPermissionActivity {
    private PermissionInfo permissionInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        permissionInfo = PermissionInfoFactory.create(UsageStatsPermissionInfo.class.getSimpleName(), this);
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    protected PermissionOperator getPermissionOperator() {
        return permissionInfo.getPermissionOperator();
    }

    @NonNull
    @Override
    protected String getPermissionTitle() {
        return permissionInfo.getPermissionTitle();
    }

    @NonNull
    @Override
    public String getPermissionDescription() {
        return permissionInfo.getPermissionDescription();
    }

    @NonNull
    @Override
    protected Class<? extends Activity> nextPageClass() {
        return permissionInfo.nextPageClass();
    }

}
