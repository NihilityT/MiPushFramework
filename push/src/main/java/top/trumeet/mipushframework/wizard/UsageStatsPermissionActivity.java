package top.trumeet.mipushframework.wizard;

import android.app.Activity;
import android.os.Build;

import androidx.annotation.NonNull;

import com.xiaomi.xmsf.R;

public class UsageStatsPermissionActivity extends RequestPermissionActivity {

    @NonNull
    @Override
    protected PermissionOperator getPermissionOperator() {
        return new UsageStatsPermissionOperator(this);
    }

    @NonNull
    @Override
    protected String getPermissionTitle() {
        return getString(R.string.wizard_title_stats_permission);
    }

    @NonNull
    @Override
    public String getPermissionDescription() {
        return getString(R.string.wizard_title_stats_permission_text);
    }

    @NonNull
    @Override
    protected Class<? extends Activity> nextPageClass() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return AlertWindowPermissionActivity.class;
        } else {
            return FinishWizardActivity.class;
        }
    }

}
