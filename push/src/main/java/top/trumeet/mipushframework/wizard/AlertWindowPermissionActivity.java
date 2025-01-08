package top.trumeet.mipushframework.wizard;

import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.M)
public class AlertWindowPermissionActivity extends RequestPermissionActivity {

    private final AlertWindowPermissionInfo alertWindowPermissionInfo = new AlertWindowPermissionInfo(this);

    @NonNull
    @Override
    protected PermissionOperator getPermissionOperator() {
        return alertWindowPermissionInfo.getPermissionOperator();
    }

    @Override
    @NonNull
    protected Intent nextPageIntent() {
        return alertWindowPermissionInfo.nextPageIntent();
    }

    @Override
    @NonNull
    protected String getPermissionTitle() {
        return alertWindowPermissionInfo.getPermissionTitle();
    }

    @Override
    @NonNull
    public String getPermissionDescription() {
        return alertWindowPermissionInfo.getPermissionDescription();
    }
}
