package com.xiaomi.xmsf;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.catchingnow.icebox.sdk_client.IceBox;
import com.xiaomi.push.service.XMPushServiceMessenger;
import com.xiaomi.xmsf.push.notification.NotificationController;
import com.xiaomi.xmsf.utils.LogUtils;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.db.EventDb;


public class ManageSpaceActivity extends PreferenceActivity {

    private static final int requestIceBoxCode = 0x233;
    private MyPreferenceFragment preferenceFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        top.trumeet.mipush.provider.DatabaseUtils.init(this);
        preferenceFragment = new MyPreferenceFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content, preferenceFragment).commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == requestIceBoxCode) {
            boolean granted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            setChecked("IceboxSupported", granted);
            Toast.makeText(getApplicationContext(),
                    getString(granted ?
                            R.string.icebox_permission_granted :
                            R.string.icebox_permission_not_granted),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void setChecked(String key, boolean granted) {
        SwitchPreference iceboxSupported = (SwitchPreference) preferenceFragment
                .getPreferenceScreen().findPreference(key);
        iceboxSupported.setChecked(granted);
    }

    public static class MyPreferenceFragment extends PreferenceFragment {

        public MyPreferenceFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.fragmented_preferences);

            Context context = getActivity();

            //Too bad in ui thread

            //TODO: Three messages seem to be too much, and need separate strings for toast.
            getPreferenceScreen().findPreference("clear_history").setOnPreferenceClickListener(preference -> {
                clearHistory(context);
                return true;
            });

            getPreferenceScreen().findPreference("clear_log").setOnPreferenceClickListener(preference -> {
                clearLog(context);
                return true;
            });


            getPreferenceScreen().findPreference("mock_notification").setOnPreferenceClickListener(preference -> {
                notifyMockNotification(context);
                return true;
            });

            SwitchPreference iceboxSupported = (SwitchPreference) getPreferenceScreen().findPreference("IceboxSupported");
            if (!isIceBoxInstalled()) {
                iceboxSupported.setEnabled(false);
                iceboxSupported.setTitle(R.string.settings_icebox_not_installed);
            } else {
                if (!iceBoxPermissionGranted(getActivity())) {
                    iceboxSupported.setChecked(false);
                }
                iceboxSupported.setOnPreferenceChangeListener((preference, newValue) -> {
                    Boolean value = (Boolean) newValue;
                    if (value && !iceBoxPermissionGranted(getActivity())) {
                        requestIceBoxPermission(getActivity());
                    }
                    return true;
                });
            }

            SwitchPreference startForegroundService = (SwitchPreference) getPreferenceScreen().findPreference("StartForegroundService");
            startForegroundService.setOnPreferenceChangeListener((preference, newValue) -> {
                startMiPushServiceAsForegroundService(getContext());
                return true;
            });
        }
    }

    private static boolean isIceBoxInstalled() {
        return Utils.isAppInstalled(IceBox.PACKAGE_NAME);
    }

    private static void requestIceBoxPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{IceBox.SDK_PERMISSION}, requestIceBoxCode);
    }

    private static boolean iceBoxPermissionGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, IceBox.SDK_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    private static void clearLog(Context context) {
        Toast.makeText(context, context.getString(R.string.settings_clear_log) + " " + context.getString(R.string.start), Toast.LENGTH_SHORT).show();
        LogUtils.clearLog(context);
        Toast.makeText(context, context.getString(R.string.settings_clear_log) + " " + context.getString(R.string.end), Toast.LENGTH_SHORT).show();
    }

    static AtomicBoolean mClearingHistory = new AtomicBoolean(false);
    private static void clearHistory(Context context) {
        if (mClearingHistory.compareAndSet(false, true)) {
            new Thread(() -> {
                Utils.makeText(context, context.getString(R.string.settings_clear_history) + " " + context.getString(R.string.start), Toast.LENGTH_SHORT);
                EventDb.deleteHistory();
                Utils.makeText(context, context.getString(R.string.settings_clear_history) + " " + context.getString(R.string.end), Toast.LENGTH_SHORT);
                mClearingHistory.set(false);
            }).start();
        }
    }

    private static void startMiPushServiceAsForegroundService(Context context) {
        LocalBroadcastManager localBroadcast = LocalBroadcastManager.getInstance(context);
        localBroadcast.sendBroadcast(new Intent(XMPushServiceMessenger.IntentStartForeground));
    }

    private static void notifyMockNotification(Context context) {
        String packageName = BuildConfig.APPLICATION_ID;
        Date date = new Date();
        String title = context.getString(R.string.debug_test_title);
        String description = context.getString(R.string.debug_test_content) + date.toString();
        NotificationController.test(context, packageName, title, description);
    }

}
