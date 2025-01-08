package top.trumeet.mipushframework.wizard;

import android.app.Activity;
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
    protected Class<? extends Activity> nextPageClass() {
        return alertWindowPermissionInfo.nextPageClass();
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
