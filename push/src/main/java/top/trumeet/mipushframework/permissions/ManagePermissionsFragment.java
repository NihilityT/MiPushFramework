package top.trumeet.mipushframework.permissions;

import static android.os.Build.VERSION_CODES.O;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
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
import com.xiaomi.xmsf.R;

import java.util.List;
import java.util.stream.Stream;

import moe.shizuku.preference.Preference;
import moe.shizuku.preference.PreferenceCategory;
import moe.shizuku.preference.PreferenceFragment;
import moe.shizuku.preference.PreferenceGroup;
import moe.shizuku.preference.PreferenceScreen;
import moe.shizuku.preference.SwitchPreferenceCompat;
import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.db.RegisteredApplicationDb;
import top.trumeet.mipush.provider.register.RegisteredApplication;
import top.trumeet.mipushframework.widgets.InfoPreference;

public class ManagePermissionsFragment extends PreferenceFragment {
    private AppConfigurationUtils appConfigurationUtils;
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
        appConfigurationUtils = new AppConfigurationUtils(getContext(), mApplicationItem);
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

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getActivity());

        addAppHeader(screen);
        addTips(screen);
        addViewRecentEventsItem(screen);
        addShowRegistrationRequestSwitch(screen);
        addNotificationManageItem(screen);

        setPreferenceScreen(screen);
    }

    private void addAppHeader(PreferenceScreen screen) {
        Context context = getContext();
        Preference appPreferenceOreo = EntityHeaderController.newInstance((AppCompatActivity) getActivity(), this, null)
                .setRecyclerView(getListView())
                .setIcon(mApplicationItem.getIcon(context))
                .setLabel(mApplicationItem.getLabel(context))
                .setSummary(mApplicationItem.getPackageName())
                .setPackageName(mApplicationItem.getPackageName())
                .setButtonActions(EntityHeaderController.ActionType.ACTION_APP_INFO, EntityHeaderController.ActionType.ACTION_NONE)
                .done((AppCompatActivity) getActivity(), context);
        screen.addPreference(appPreferenceOreo);
    }

    private void addNotificationManageItem(PreferenceScreen screen) {
        if (Build.VERSION.SDK_INT >= O) {
            addAllNotificationChannelsTo(screen);
        } else {
            addManageNotificationItem(screen);
        }
    }

    private void addTips(PreferenceScreen screen) {
        boolean shouldSuggestFakeApp = appConfigurationUtils.shouldSuggestFakeApp(mApplicationItem.getPackageName());

        int registeredType = mApplicationItem.getRegisteredType();
        if (registeredType == RegisteredApplication.RegisteredType.NotRegistered) {
            addNotRegisteredTips(shouldSuggestFakeApp, screen);
        } else if (registeredType == RegisteredApplication.RegisteredType.Unregistered) {
            addAbnormalTips(screen);
        }
    }

    private void addShowRegistrationRequestSwitch(PreferenceScreen screen) {
        addItem(mApplicationItem.isNotificationOnRegister(), (preference, newValue) -> {
            mApplicationItem.setNotificationOnRegister(((Boolean) newValue));
            return true;
        }, getString(R.string.permission_notification_on_register), getString(R.string.permission_summary_notification_on_register), screen);
    }

    private void addViewRecentEventsItem(PreferenceScreen screen) {
        Preference viewRecentActivityPreference = new Preference(getActivity());
        viewRecentActivityPreference.setTitle(R.string.recent_activity_view);
        viewRecentActivityPreference.setOnPreferenceClickListener(preference -> {
            appConfigurationUtils.gotoRecentEventsPage();
            return true;
        });

        screen.addPreference(viewRecentActivityPreference);
    }

    private void addAllNotificationChannelsTo(PreferenceScreen screen) {
        List<NotificationChannelGroup> groups = appConfigurationUtils.getNotificationChannelGroups();
        List<NotificationChannel> notificationChannels = appConfigurationUtils.getNotificationChannels();
        groups.forEach(group -> {
            String categoryName = appConfigurationUtils.getNotificationCategoryName(group);
            addNotificationCategoryTo(screen,
                    categoryName,
                    notificationChannels.stream().filter(notificationChannel -> TextUtils.equals(notificationChannel.getGroup(), group.getId())));
        });
    }

    private void addManageNotificationItem(PreferenceScreen screen) {
        PreferenceCategory notificationChannelsCategory = new PreferenceCategory(getActivity(), null,
                moe.shizuku.preference.R.attr.preferenceCategoryStyle, R.style.Preference_Category_Material);
        notificationChannelsCategory.setTitle(R.string.notification_channels);
        screen.addPreference(notificationChannelsCategory);

        Preference manageNotificationPreference = getManageNotificationPreference();
        notificationChannelsCategory.addPreference(manageNotificationPreference);
    }

    private @NonNull Preference getManageNotificationPreference() {
        Preference manageNotificationPreference = new Preference(getActivity());
        manageNotificationPreference.setTitle(R.string.settings_manage_app_notifications);
        manageNotificationPreference.setSummary(R.string.settings_manage_app_notifications_summary);
        manageNotificationPreference.setOnPreferenceClickListener(preference -> {
            appConfigurationUtils.gotoNotificationSettingPage();
            return true;
        });
        if (mApplicationItem.getRegisteredType() == RegisteredApplication.RegisteredType.NotRegistered) {
            manageNotificationPreference.setEnabled(false);
        }
        return manageNotificationPreference;
    }

    private void addNotRegisteredTips(boolean shouldSuggestFakeApp, PreferenceScreen screen) {
        int notRegisteredDesc = shouldSuggestFakeApp
                ? R.string.status_app_not_registered_detail_with_fake_suggest
                : R.string.status_app_not_registered_detail_without_fake_suggest;
        addTips(R.string.status_app_not_registered_title, notRegisteredDesc, screen);
    }

    private void addAbnormalTips(PreferenceScreen screen) {
        addTips(R.string.status_app_registered_error_title, R.string.status_app_registered_error_desc, screen);
    }

    private void addTips(int status_app_registered_error_title, int status_app_registered_error_desc, PreferenceScreen screen) {
        InfoPreference preferenceStatus = new InfoPreference(getActivity(), null,
                moe.shizuku.preference.R.attr.preferenceStyle, R.style.Preference_Material);
        preferenceStatus.setTitle(getString(status_app_registered_error_title));
        preferenceStatus.setSummary(Html.fromHtml(getString(status_app_registered_error_desc)));
        Drawable iconError = ContextCompat.getDrawable(getActivity(), R.drawable.ic_error_outline_black_24dp);
        DrawableCompat.setTint(iconError, Color.parseColor("#D50000"));
        preferenceStatus.setIcon(iconError);
        screen.addPreference(preferenceStatus);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void addNotificationCategoryTo(PreferenceScreen screen, String categoryName, Stream<NotificationChannel> notificationChannelStream) {
        PreferenceCategory notificationChannelsCategory = new PreferenceCategory(getActivity(), null, moe.shizuku.preference.R.attr.preferenceCategoryStyle, R.style.Preference_Category_Material);
        notificationChannelsCategory.setTitle(categoryName);
        screen.addPreference(notificationChannelsCategory);

        String configApp = appConfigurationUtils.getConfigApp();
        notificationChannelStream.forEach(channel -> {
            addNotificationChannelItem(channel, notificationChannelsCategory, configApp);
        });
    }

    private void addNotificationChannelItem(NotificationChannel channel, PreferenceCategory notificationChannelsCategory, String configApp) {
        Preference item = new Preference(getActivity());

        CharSequence title = AppConfigurationUtils.getNotificationTitle(channel);
        item.setTitle(title);

        String summary = AppConfigurationUtils.getNotificationSummary(channel);
        item.setSummary(summary);

        item.setOnPreferenceClickListener(preference -> {
            AlertDialog.Builder build = new AlertDialog.Builder(getContext())
                    .setTitle(channel.getName())
                    .setNegativeButton(R.string.notification_channels_delete, (dialogInterface, i) -> {
                        appConfigurationUtils.deleteNotificationChannel(channel);
                        notificationChannelsCategory.removePreference(item);
                    }).setNeutralButton(R.string.notification_channels_copy_id, (dialogInterface, i) -> {
                        appConfigurationUtils.copyToClipboard(channel);
                    }).setPositiveButton(R.string.notification_channels_setting, (dialogInterface, i) -> {
                        appConfigurationUtils.gotoNotificationChannelSettingPage(channel, configApp);
                    });
            build.create().show();
            return true;
        });
        notificationChannelsCategory.addPreference(item);
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
