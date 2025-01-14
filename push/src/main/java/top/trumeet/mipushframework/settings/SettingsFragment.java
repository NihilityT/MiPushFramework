package top.trumeet.mipushframework.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.nihility.InternalMessenger;
import com.xiaomi.push.service.XMPushServiceMessenger;
import com.xiaomi.xmsf.R;
import com.xiaomi.xmsf.SettingUtils;

import moe.shizuku.preference.Preference;
import moe.shizuku.preference.PreferenceFragment;
import top.trumeet.mipushframework.MainActivity;

/**
 * Created by Trumeet on 2017/8/27.
 * Main settings
 *
 * @author Trumeet
 * @see MainActivity
 */

public class SettingsFragment extends PreferenceFragment {

    private static final String TAG = SettingsFragment.class.getSimpleName();
    private static final Logger logger = XLog.tag(TAG).build();

    private final InternalMessenger messenger = new InternalMessenger(getContext()) {{
        register(new IntentFilter(XMPushServiceMessenger.IntentSetConnectionStatus));
        addListener(intent -> {
            String host = intent.getStringExtra("host");
            if (TextUtils.isEmpty(host)) {
                return;
            }
            Preference preference = getPreference("XMPP_server");
            String summary = preference.getSummary().toString();
            preference.setSummary(summary.replaceFirst(
                    "(?m)$[\\s\\S]*", String.format("\nCurrent: [%s]", host)));
        });
    }};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateConnectionStatus();
    }

    private void updateConnectionStatus() {
        messenger.send(new Intent(XMPushServiceMessenger.IntentGetConnectionStatus));
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);
        setPreferenceOnclick("key_get_log", preference -> {
            SettingUtils.shareLogs(getContext());
            return true;
        });
        {
            Uri treeUri = SettingUtils.getConfigurationDirectory(getActivity());
            Preference preference = getPreference("configuration_directory");
            if (treeUri != null) {
                preference.setSummary(treeUri.toString());
            }
            preference.setOnPreferenceClickListener(pref -> {
                openDirectory(getActivity());
                return true;
            });
        }
        {
            String host = SettingUtils.getXMPPServer(getActivity());
            Preference preference = getPreference("XMPP_server");
            if (!TextUtils.isEmpty(host)) {
                preference.setSummary(host);
            }
            preference.setOnPreferenceClickListener(pref -> {
                EditText editText = getXMPPServerAddrEditText();
                AlertDialog.Builder build = new AlertDialog.Builder(getActivity())
                        .setView(editText)
                        .setTitle(R.string.settings_XMPP_server)
                        .setCancelable(true)
                        .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                            String newHost = String.valueOf(editText.getText()).trim();
                            SettingUtils.setXMPPServer(getActivity(), newHost);
                            if (TextUtils.isEmpty(newHost)) {
                                preference.setSummary(R.string.settings_XMPP_server_summary);
                            } else {
                                preference.setSummary(newHost);
                            }
                            SettingUtils.sendXMPPReconnectRequest(getContext());
                        });
                build.create().show();
                return true;
            });
        }
        {
            Preference preference = new Preference(getContext());
            preference.setOnPreferenceClickListener(pref -> {
                SettingUtils.tryForceRegisterAllApplications();
                return true;
            });
            preference.setTitle(getString(R.string.try_to_force_register_all_applications));
            getPreferenceScreen().addPreference(preference);
        }
    }

    private @NonNull EditText getXMPPServerAddrEditText() {
        EditText editText = new EditText(getActivity());
        editText.setHint(SettingUtils.getXMPPServerHint());
        editText.setText(SettingUtils.getXMPPServer(getActivity()));
        editText.setSingleLine();
        return editText;
    }

    private void setPreferenceOnclick(String key, Preference.OnPreferenceClickListener onPreferenceClickListener) {
        getPreference(key).setOnPreferenceClickListener(onPreferenceClickListener);
    }

    private Preference getPreference(String key) {
        return getPreferenceScreen().findPreference(key);
    }

    @Override
    public void onStart() {
        super.onStart();
        long time = System.currentTimeMillis();
        Log.d(TAG, "rebuild UI took: " + (System.currentTimeMillis() -
                time));
    }

    private void openDirectory(Context context) {
        openDirectory(Uri.fromFile(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)));
    }

    private void openDirectory(Uri uriToLoad) {
        // Choose a directory using the system's file picker.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when it loads.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uriToLoad);
        }

        startActivityForResult(intent, 123);
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 123 && resultCode == Activity.RESULT_OK && data != null) {
            try {
                Uri uri = SettingUtils.saveConfigurationUri(getContext(), data);
                Preference preference = getPreference("configuration_directory");
                preference.setSummary(uri.toString());
            } catch (Throwable e) {
                logger.e("onActivityResult configuration", e);
            }
        }
    }

}
