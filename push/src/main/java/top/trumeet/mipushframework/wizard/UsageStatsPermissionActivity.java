package top.trumeet.mipushframework.wizard;

import android.app.Activity;

import androidx.annotation.NonNull;

public class UsageStatsPermissionActivity extends RequestPermissionActivity {

    private final PermissionInfo permissionInfo = new UsageStatsPermissionInfo(this);

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
