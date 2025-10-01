package top.trumeet.mipushframework.wizard.permission;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.RequiresApi;

import top.trumeet.common.push.PushServiceAccessibility;

@RequiresApi(api = Build.VERSION_CODES.M)
public class RequestIgnoreBatteryOptimizationsPermissionOperator implements PermissionOperator {
    private final Context context;

    RequestIgnoreBatteryOptimizationsPermissionOperator(Context context) {
        this.context = context;
    }

    @Override
    public boolean isPermissionGranted() {
        return PushServiceAccessibility.isInDozeWhiteList(context);
    }

    @Override
    public void requestPermissionSilently() {

    }

    @Override
    public void requestPermission() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
    }
}
