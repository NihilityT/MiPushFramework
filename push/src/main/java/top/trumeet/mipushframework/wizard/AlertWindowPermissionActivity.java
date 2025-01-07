package top.trumeet.mipushframework.wizard;

import android.app.Activity;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.xiaomi.xmsf.R;

@RequiresApi(api = Build.VERSION_CODES.M)
public class AlertWindowPermissionActivity extends RequestPermissionActivity {

    @NonNull
    @Override
    protected PermissionOperator getPermissionOperator() {
        return new AlertWindowPermissionOperator(this);
    }

    @Override
    @NonNull
    protected Class<? extends Activity> nextPageClass() {
        return FinishWizardActivity.class;
    }

    @Override
    @NonNull
    protected String getPermissionTitle() {
        return getString(R.string.wizard_title_alert_window_permission);
    }

    @Override
    @NonNull
    public String getPermissionDescription() {
        return getString(R.string.wizard_title_alert_window_text);
    }
}
