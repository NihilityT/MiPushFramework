package top.trumeet.mipushframework.wizard;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.xiaomi.xmsf.R;

import top.trumeet.common.override.AppOpsManagerOverride;
import top.trumeet.common.utils.Utils;
import top.trumeet.mipushframework.utils.PermissionUtils;

public class UsageStatsPermissionOperator implements PermissionOperator {
    final private Context context;

    public UsageStatsPermissionOperator(Context context) {
        this.context = context;
    }

    @Override
    public boolean isPermissionGranted() {
        int result = Utils.checkOp(context, AppOpsManagerOverride.OP_GET_USAGE_STATS);
        return (result == AppOpsManager.MODE_ALLOWED);
    }

    @Override
    public void requestPermissionSilently() {
        PermissionUtils.lunchAppOps(context,
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                context.getString(R.string.wizard_title_stats_permission_text));
    }

    @Override
    public void requestPermission() {
        context.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }
}