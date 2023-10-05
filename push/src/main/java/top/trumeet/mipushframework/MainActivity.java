package top.trumeet.mipushframework;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.Toast;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.elevation.SurfaceColors;
import com.xiaomi.channel.commonutils.android.DeviceInfo;
import com.xiaomi.channel.commonutils.android.MIUIUtils;
import com.xiaomi.push.service.XMPushServiceAspect;
import com.xiaomi.smack.ConnectionConfiguration;
import com.xiaomi.xmsf.R;
import com.xiaomi.xmsf.utils.ConfigCenter;

import org.aspectj.lang.annotation.Aspect;

import io.reactivex.disposables.CompositeDisposable;
import top.trumeet.mipushframework.control.CheckPermissionsUtils;

/**
 * Main activity
 *
 * @author Trumeet
 */
@Aspect
public abstract class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private View mConnectProgress;
    private ViewPropertyAnimator mProgressFadeOutAnimate;
    private MainFragment mFragment;
    private CompositeDisposable composite = new CompositeDisposable();
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra("status");
            setTitle(getString(R.string.preference_title) + " (" + status + ")");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hookTest();
        checkAndConnect();

        ConfigCenter.getInstance().loadConfigurations(this);

        int color = SurfaceColors.SURFACE_2.getColor(this);
        getWindow().setStatusBarColor(color);
        getWindow().setNavigationBarColor(color);

        LocalBroadcastManager localBroadcast = LocalBroadcastManager.getInstance(this);
        localBroadcast.registerReceiver(mMessageReceiver, new IntentFilter(XMPushServiceAspect.IntentSetConnectionStatus));
        localBroadcast.sendBroadcast(new Intent(XMPushServiceAspect.IntentGetConnectionStatus));
    }


    void hookTest() {
        Log.i(TAG, String.format("[hook_res] MIUIUtils.getIsMIUI() -> [%s]", MIUIUtils.getIsMIUI()));
        Log.i(TAG, String.format("[hook_res] DeviceInfo.quicklyGetIMEI() -> [%s]", DeviceInfo.quicklyGetIMEI(this)));
        Log.i(TAG, String.format("[hook_res] DeviceInfo.getMacAddress() -> [%s]", DeviceInfo.getMacAddress(this)));
        Log.i(TAG, String.format("[hook_res] ConnectionConfiguration.getXmppServerHost() -> [%s]", ConnectionConfiguration.getXmppServerHost()));
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @UiThread
    private void checkAndConnect() {
        Log.d("MainActivity", "checkAndConnect");
        composite.add(CheckPermissionsUtils.checkAndRun(result -> {
            switch (result) {
                case OK:
                    initRealView();
                    break;
                case PERMISSION_NEEDED:
                    Toast.makeText(this, getString(top.trumeet.common.R.string.request_permission), Toast.LENGTH_LONG)
                            .show();
                    // Restart to request permissions again.
                    checkAndConnect();
                    break;
                case PERMISSION_NEEDED_SHOW_SETTINGS:
                    Toast.makeText(this, getString(top.trumeet.common.R.string.request_permission), Toast.LENGTH_LONG)
                            .show();
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(uri)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    break;
                case REMOVE_DOZE_NEEDED:
                    Toast.makeText(this, getString(R.string.request_battery_whitelist), Toast.LENGTH_LONG)
                            .show();
                    checkAndConnect();
                    break;
            }
        }, throwable -> {
            Log.e(TAG, "check permissions", throwable);
        }, this));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfiguration) {
        super.onConfigurationChanged(newConfiguration);
    }

    private void initRealView() {
        if (mConnectProgress != null) {
            mConnectProgress.setVisibility(View.GONE);
            mConnectProgress = null;
        }

        mFragment = new MainFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, mFragment)
                .commitAllowingStateLoss();

    }

    @Override
    public void onDestroy() {

        if (mProgressFadeOutAnimate != null) {
            mProgressFadeOutAnimate.cancel();
            mProgressFadeOutAnimate = null;
        }
        // Activity request should cancel in onPause?
        if (composite != null && !composite.isDisposed()) {
            composite.dispose();
        }
        super.onDestroy();
    }

}
