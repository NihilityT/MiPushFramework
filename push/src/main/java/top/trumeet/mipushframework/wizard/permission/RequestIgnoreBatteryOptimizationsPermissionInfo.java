package top.trumeet.mipushframework.wizard.permission;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.xiaomi.xmsf.R;

import top.trumeet.mipushframework.wizard.RequestPermissionActivity;

@RequiresApi(api = Build.VERSION_CODES.M)
public class RequestIgnoreBatteryOptimizationsPermissionInfo implements PermissionInfo {

    private final Context context;

    public RequestIgnoreBatteryOptimizationsPermissionInfo(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public PermissionOperator getPermissionOperator() {
        return new RequestIgnoreBatteryOptimizationsPermissionOperator(context);
    }

    @Override
    @NonNull
    public Intent nextPageIntent() {
        return RequestPermissionActivity.intentFor(UsageStatsPermissionInfo.class);
    }

    @Override
    @NonNull
    public String getPermissionTitle() {
        return context.getString(R.string.wizard_title_ignore_battery_optimizations_permission);
    }

    @Override
    @NonNull
    public String getPermissionDescription() {
        return context.getString(R.string.wizard_title_ignore_battery_optimizations_text);
    }
}
