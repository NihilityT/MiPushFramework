package top.trumeet.mipushframework.wizard.permission;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.xiaomi.xmsf.R;

@RequiresApi(api = Build.VERSION_CODES.M)
public class AlertWindowPermissionInfo implements PermissionInfo {
    private final Context context;

    public AlertWindowPermissionInfo(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public PermissionOperator getPermissionOperator() {
        return new AlertWindowPermissionOperator(context);
    }

    @Override
    @NonNull
    public String getPermissionTitle() {
        return context.getString(R.string.wizard_title_alert_window_permission);
    }

    @Override
    @NonNull
    public String getPermissionDescription() {
        return context.getString(R.string.wizard_title_alert_window_text);
    }
}