package top.trumeet.mipushframework.permissions;

import static android.os.Build.VERSION_CODES.O;
import static android.provider.Settings.EXTRA_APP_PACKAGE;
import static android.provider.Settings.EXTRA_CHANNEL_ID;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.android.settings.widget.EntityHeaderController;
import com.nihility.notification.NotificationManagerEx;
import com.xiaomi.xmsf.R;
import com.xiaomi.xmsf.push.notification.NotificationChannelManager;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import moe.shizuku.preference.Preference;
import moe.shizuku.preference.PreferenceCategory;
import moe.shizuku.preference.PreferenceFragment;
import moe.shizuku.preference.PreferenceGroup;
import moe.shizuku.preference.PreferenceScreen;
import moe.shizuku.preference.SwitchPreferenceCompat;
import top.trumeet.common.Constants;
import top.trumeet.common.utils.NotificationUtils;
import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.db.RegisteredApplicationDb;
import top.trumeet.mipush.provider.register.RegisteredApplication;
import top.trumeet.mipushframework.event.RecentActivityActivity;
import top.trumeet.mipushframework.widgets.InfoPreference;

public class ManagePermissionsFragment extends PreferenceFragment {
    private RegisteredApplication mApplicationItem;
    private SaveApplicationInfoTask mSaveApplicationInfoTask;
    private MenuItem menuOk;
    // Will be used in SaveTask, null = not changed
    // Isn't a good idea
    private Boolean changeFakeSettings = null;

    public ManagePermissionsFragment(RegisteredApplication application) {
        this.mApplicationItem = application;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menuOk = menu.add(0, 0, 0, R.string.apply);
        Drawable iconOk = ContextCompat.getDrawable(getActivity(), R.drawable.ic_check_black_24dp);
        DrawableCompat.setTint(iconOk, Utils.getColorAttr(getContext(), R.attr.colorAccent));
        menuOk.setIcon(iconOk);
        menuOk.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 0) {
            if (mSaveApplicationInfoTask != null && !mSaveApplicationInfoTask.isCancelled()) {
                return true;
            }
            mSaveApplicationInfoTask = new SaveApplicationInfoTask();
            mSaveApplicationInfoTask.execute();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDetach() {
        if (mSaveApplicationInfoTask != null && !mSaveApplicationInfoTask.isCancelled()) {
            mSaveApplicationInfoTask.cancel(true);
            mSaveApplicationInfoTask = null;
        }
        super.onDetach();
    }

    private boolean suggestEnableFake(String pkg) {
        List<String> pkgsEqual = Arrays.asList(getResources().getStringArray(R.array.fake_blacklist_equals));
        if (pkgsEqual.contains(pkg)) return false;
        List<String> pkgsContains = Arrays.asList(getResources().getStringArray(R.array.fake_blacklist_contains));
        for (String p : pkgsContains)
            if (pkg.contains(p)) return false;
        if (!Utils.isUserApplication(pkg)) return false;
        return true;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getActivity());

        Preference appPreferenceOreo = EntityHeaderController.newInstance((AppCompatActivity) getActivity(), this, null).setRecyclerView(getListView()).setIcon(mApplicationItem.getIcon(getContext())).setLabel(mApplicationItem.getLabel(getContext())).setSummary(mApplicationItem.getPackageName()).setPackageName(mApplicationItem.getPackageName()).setButtonActions(EntityHeaderController.ActionType.ACTION_APP_INFO, EntityHeaderController.ActionType.ACTION_NONE).done((AppCompatActivity) getActivity(), getContext());
        screen.addPreference(appPreferenceOreo);

        boolean suggestFake = suggestEnableFake(mApplicationItem.getPackageName());

        if (mApplicationItem.getRegisteredType() == 0) {
            InfoPreference preferenceStatus = new InfoPreference(getActivity(), null, moe.shizuku.preference.R.attr.preferenceStyle, R.style.Preference_Material);
            preferenceStatus.setTitle(getString(R.string.status_app_not_registered_title));
            preferenceStatus.setSummary(Html.fromHtml(getString(suggestFake ? R.string.status_app_not_registered_detail_with_fake_suggest : R.string.status_app_not_registered_detail_without_fake_suggest)));
            Drawable iconError = ContextCompat.getDrawable(getActivity(), R.drawable.ic_error_outline_black_24dp);
            DrawableCompat.setTint(iconError, Color.parseColor("#D50000"));
            preferenceStatus.setIcon(iconError);
            screen.addPreference(preferenceStatus);
        }
        if (mApplicationItem.getRegisteredType() == RegisteredApplication.RegisteredType.Unregistered) {
            InfoPreference preferenceStatus = new InfoPreference(getActivity(), null, moe.shizuku.preference.R.attr.preferenceStyle, R.style.Preference_Material);
            preferenceStatus.setTitle(getString(R.string.status_app_registered_error_title));
            preferenceStatus.setSummary(Html.fromHtml(getString(R.string.status_app_registered_error_desc)));
            Drawable iconError = ContextCompat.getDrawable(getActivity(), R.drawable.ic_error_outline_black_24dp);
            DrawableCompat.setTint(iconError, Color.parseColor("#D50000"));
            preferenceStatus.setIcon(iconError);
            screen.addPreference(preferenceStatus);
        }

        Preference viewRecentActivityPreference = new Preference(getActivity());
        viewRecentActivityPreference.setTitle(R.string.recent_activity_view);
        viewRecentActivityPreference.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getActivity(), RecentActivityActivity.class).setData(Uri.parse(mApplicationItem.getPackageName())));
            return true;
        });

        screen.addPreference(viewRecentActivityPreference);

        addItem(mApplicationItem.isNotificationOnRegister(), (preference, newValue) -> {
            mApplicationItem.setNotificationOnRegister(((Boolean) newValue));
            return true;
        }, getString(R.string.permission_notification_on_register), getString(R.string.permission_summary_notification_on_register), screen);

        if (Build.VERSION.SDK_INT >= O) {
            String mipushGroup = NotificationUtils.getGroupIdByPkg(mApplicationItem.getPackageName());

            List<NotificationChannelGroup> groups = NotificationManagerEx.INSTANCE.getNotificationChannelGroups(mApplicationItem.getPackageName());
            if (NotificationManagerEx.isHooked) {
                groups.sort((lhs, rhs) -> {
                    if (TextUtils.equals(lhs.getId(), mipushGroup) || rhs.getId() == null) {
                        return -1;
                    }
                    if (TextUtils.equals(rhs.getId(), mipushGroup) || lhs.getId() == null) {
                        return 1;
                    }

                    return lhs.getId().compareTo(rhs.getId());
                });
            } else {
                groups.removeIf(group -> !TextUtils.equals(group.getId(), mipushGroup));
            }
            groups.forEach(group -> {
                String suffix = group.getId() == null ? "" : String.format(": %s (%s)", group.getName(), group.getId());
                addNotificationCategory(screen, getString(R.string.notification_channels) + suffix, notificationChannel -> TextUtils.equals(notificationChannel.getGroup(), group.getId()));
            });

        } else {
            PreferenceCategory notificationChannelsCategory = new PreferenceCategory(getActivity(), null, moe.shizuku.preference.R.attr.preferenceCategoryStyle, R.style.Preference_Category_Material);
            notificationChannelsCategory.setTitle(R.string.notification_channels);
            screen.addPreference(notificationChannelsCategory);

            Preference manageNotificationPreference = new Preference(getActivity());
            manageNotificationPreference.setTitle(R.string.settings_manage_app_notifications);
            manageNotificationPreference.setSummary(R.string.settings_manage_app_notifications_summary);
            manageNotificationPreference.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(EXTRA_APP_PACKAGE, Constants.SERVICE_APP_NAME));
                return true;
            });
            if (mApplicationItem.getRegisteredType() == 0) {
                manageNotificationPreference.setEnabled(false);
            }
            notificationChannelsCategory.addPreference(manageNotificationPreference);
        }

        setPreferenceScreen(screen);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void addNotificationCategory(PreferenceScreen screen, String categoryName, Predicate<NotificationChannel> predicate) {
        PreferenceCategory notificationChannelsCategory = new PreferenceCategory(getActivity(), null, moe.shizuku.preference.R.attr.preferenceCategoryStyle, R.style.Preference_Category_Material);
        notificationChannelsCategory.setTitle(categoryName);
        screen.addPreference(notificationChannelsCategory);

        String configApp = NotificationManagerEx.isHooked ? mApplicationItem.getPackageName() : Constants.SERVICE_APP_NAME;
        List<NotificationChannel> notificationChannels = NotificationManagerEx.INSTANCE.getNotificationChannels(mApplicationItem.getPackageName());
        notificationChannels.stream().filter(predicate).forEach(channel -> {
            Preference item = new Preference(getActivity());

            CharSequence title = channel.getName();
            if (!NotificationChannelManager.isNotificationChannelEnabled(channel)) {
                title = "[disable]" + title;
            }
            item.setTitle(title);

            String summary = "id: " + channel.getId();
            String description = channel.getDescription();
            if (!TextUtils.isEmpty(description)) {
                summary += "\n" + description;
            }
            item.setSummary(summary);

            item.setOnPreferenceClickListener(preference -> {
                AlertDialog.Builder build = new AlertDialog.Builder(getContext()).setTitle(channel.getName()).setNegativeButton(R.string.notification_channels_delete, (dialogInterface, i) -> {
                    NotificationManagerEx.INSTANCE.deleteNotificationChannel(mApplicationItem.getPackageName(), channel.getId());
                    notificationChannelsCategory.removePreference(item);
                }).setNeutralButton(R.string.notification_channels_copy_id, (dialogInterface, i) -> {
                    ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboardManager.setText(channel.getId());
                }).setPositiveButton(R.string.notification_channels_setting, (dialogInterface, i) -> {
                    startActivity(new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(EXTRA_APP_PACKAGE, configApp).putExtra(EXTRA_CHANNEL_ID, channel.getId()));
                });
                build.create().show();
                return true;
            });
            notificationChannelsCategory.addPreference(item);
        });
    }

    private SwitchPreferenceCompat addItem(boolean value, Preference.OnPreferenceChangeListener listener, CharSequence title, CharSequence summary, PreferenceGroup parent) {
        SwitchPreferenceCompat preference = createPreference(value, listener, title, summary);
        parent.addPreference(preference);

        return preference;
    }

    @NonNull
    private SwitchPreferenceCompat createPreference(boolean value, Preference.OnPreferenceChangeListener listener, CharSequence title, CharSequence summary) {
        SwitchPreferenceCompat preference = new SwitchPreferenceCompat(getActivity(), null, moe.shizuku.preference.R.attr.switchPreferenceStyle, R.style.Preference_SwitchPreferenceCompat);
        preference.setOnPreferenceChangeListener(listener);
        preference.setTitle(title);
        preference.setSummary(summary);
        preference.setChecked(value);
        return preference;
    }

    /**
     * @deprecated Use {@link #addItem(boolean, Preference.OnPreferenceChangeListener, CharSequence, CharSequence, PreferenceGroup)} instead.
     */
    @Deprecated
    private void addItem(boolean value, Preference.OnPreferenceChangeListener listener, CharSequence title, PreferenceGroup parent) {
        addItem(value, listener, title, null, parent);
    }

    private class SaveApplicationInfoTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (mApplicationItem != null && mApplicationItem.getRegisteredType() != 0) {
                RegisteredApplicationDb.update(mApplicationItem);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            getActivity().finish();
        }
    }
}
