package top.trumeet.mipushframework.wizard.permission;

import android.content.Context;

import androidx.annotation.NonNull;

import com.xiaomi.xmsf.R;

public class UsageStatsPermissionInfo implements PermissionInfo {
    Context context;

    public UsageStatsPermissionInfo(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public PermissionOperator getPermissionOperator() {
        return new UsageStatsPermissionOperator(context);
    }

    @NonNull
    @Override
    public String getPermissionTitle() {
        return context.getString(R.string.wizard_title_stats_permission);
    }

    @NonNull
    @Override
    public String getPermissionDescription() {
        return context.getString(R.string.wizard_title_stats_permission_text);
    }

}