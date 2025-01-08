package top.trumeet.mipushframework.wizard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

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

    @NonNull
    @Override
    public Intent nextPageIntent() {
        return new Intent(context, nextPageClass());
    }

    @NonNull
    public Class<? extends Activity> nextPageClass() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return AlertWindowPermissionActivity.class;
        } else {
            return FinishWizardActivity.class;
        }
    }
}