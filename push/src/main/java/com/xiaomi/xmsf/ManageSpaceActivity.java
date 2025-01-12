package com.xiaomi.xmsf;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.widget.Toast;

import androidx.annotation.NonNull;


public class ManageSpaceActivity extends PreferenceActivity {

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
        if (requestCode == SettingUtils.requestIceBoxCode) {
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
                SettingUtils.clearHistory(context);
                return true;
            });

            getPreferenceScreen().findPreference("clear_log").setOnPreferenceClickListener(preference -> {
                SettingUtils.clearLog(context);
                return true;
            });


            getPreferenceScreen().findPreference("mock_notification").setOnPreferenceClickListener(preference -> {
                SettingUtils.notifyMockNotification(context);
                return true;
            });

            SwitchPreference iceboxSupported = (SwitchPreference) getPreferenceScreen().findPreference("IceboxSupported");
            if (!SettingUtils.isIceBoxInstalled()) {
                iceboxSupported.setEnabled(false);
                iceboxSupported.setTitle(R.string.settings_icebox_not_installed);
            } else {
                if (!SettingUtils.iceBoxPermissionGranted(getActivity())) {
                    iceboxSupported.setChecked(false);
                }
                iceboxSupported.setOnPreferenceChangeListener((preference, newValue) -> {
                    Boolean value = (Boolean) newValue;
                    if (value && !SettingUtils.iceBoxPermissionGranted(getActivity())) {
                        SettingUtils.requestIceBoxPermission(getActivity());
                    }
                    return true;
                });
            }

            SwitchPreference startForegroundService = (SwitchPreference) getPreferenceScreen().findPreference("StartForegroundService");
            startForegroundService.setOnPreferenceChangeListener((preference, newValue) -> {
                SettingUtils.startMiPushServiceAsForegroundService(getContext());
                return true;
            });
        }
    }

}
