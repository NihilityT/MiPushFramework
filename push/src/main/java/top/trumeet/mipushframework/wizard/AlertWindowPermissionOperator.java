package top.trumeet.mipushframework.wizard;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.RequiresApi;

import com.xiaomi.xmsf.R;

import top.trumeet.mipushframework.utils.PermissionUtils;

@RequiresApi(api = Build.VERSION_CODES.M)
public class AlertWindowPermissionOperator implements PermissionOperator {
    final private Context context;

    public AlertWindowPermissionOperator(Context context) {
        this.context = context;
    }

    @Override
    public boolean isPermissionGranted() {
        return Settings.canDrawOverlays(context);
    }

    @Override
    public void requestPermissionSilently() {
        PermissionUtils.lunchAppOps(context,
                AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                context.getString(R.string.wizard_title_alert_window_text));
    }

    @Override
    public void requestPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
    }
}