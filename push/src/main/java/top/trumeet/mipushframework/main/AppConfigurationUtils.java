package top.trumeet.mipushframework.main;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nihility.notification.NotificationManagerEx;
import com.xiaomi.xmsf.R;
import com.xiaomi.xmsf.push.notification.NotificationChannelManager;

import java.util.Arrays;
import java.util.List;

import top.trumeet.common.Constants;
import top.trumeet.common.utils.NotificationUtils;
import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.entities.RegisteredApplication;

public class AppConfigurationUtils {
    private final Context context;
    private final RegisteredApplication application;

    public AppConfigurationUtils(Context context, RegisteredApplication application) {
        this.context = context;
        this.application = application;
    }

    boolean shouldSuggestFakeApp(String pkg) {
        return !isBlacklistApp(pkg) && Utils.isUserApplication(pkg);
    }

    boolean isBlacklistApp(String pkg) {
        return isBlacklistContaines(pkg) || isBlacklistMatches(pkg);
    }

    boolean isBlacklistMatches(String pkg) {
        String[] pkgsContains = context.getResources().getStringArray(R.array.fake_blacklist_contains);
        for (String p : pkgsContains)
            if (pkg.contains(p))
                return true;
        return false;
    }

    boolean isBlacklistContaines(String pkg) {
        List<String> pkgsEqual = Arrays.asList(context.getResources().getStringArray(R.array.fake_blacklist_equals));
        if (pkgsEqual.contains(pkg))
            return true;
        return false;
    }

    void gotoRecentEventsPage() {
        context.startActivity(
                new Intent(context, RecentEventListPage.class)
                        .setData(Uri.parse(application.getPackageName())));
    }

    void gotoNotificationSettingPage() {
        context.startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, Constants.SERVICE_APP_NAME));
    }

    static void removeAllNonMIPushGroup(List<NotificationChannelGroup> groups, String mipushGroup) {
        groups.removeIf(group -> !TextUtils.equals(group.getId(), mipushGroup));
    }

    static void makeMIPushGroupToTopPositions(List<NotificationChannelGroup> groups, String mipushGroup) {
        groups.sort((lhs, rhs) -> {
            if (TextUtils.equals(lhs.getId(), mipushGroup) || rhs.getId() == null) {
                return -1;
            }
            if (TextUtils.equals(rhs.getId(), mipushGroup) || lhs.getId() == null) {
                return 1;
            }

            return lhs.getId().compareTo(rhs.getId());
        });
    }

    void gotoNotificationChannelSettingPage(NotificationChannel channel, String configApp) {
        context.startActivity(new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, configApp)
                .putExtra(Settings.EXTRA_CHANNEL_ID, channel.getId()));
    }

    void copyToClipboard(NotificationChannel channel) {
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.setText(channel.getId());
    }

    static CharSequence getNotificationTitle(NotificationChannel channel) {
        CharSequence title = channel.getName();
        if (!NotificationChannelManager.isNotificationChannelEnabled(channel)) {
            title = "[disable]" + title;
        }
        return title;
    }

    static @NonNull String getNotificationSummary(NotificationChannel channel) {
        String summary = "id: " + channel.getId();
        String description = channel.getDescription();
        if (!TextUtils.isEmpty(description)) {
            summary += "\n" + description;
        }
        return summary;
    }

    void deleteNotificationChannel(NotificationChannel channel) {
        NotificationManagerEx.INSTANCE.deleteNotificationChannel(application.getPackageName(), channel.getId());
    }

    @Nullable
    List<NotificationChannel> getNotificationChannels() {
        return NotificationManagerEx.INSTANCE.getNotificationChannels(application.getPackageName());
    }

    String getConfigApp() {
        return NotificationManagerEx.isHooked ? application.getPackageName() : Constants.SERVICE_APP_NAME;
    }

    @NonNull String getNotificationCategoryName(NotificationChannelGroup group) {
        String suffix = group.getId() == null ? "" : String.format(": %s (%s)", group.getName(), group.getId());
        String categoryName = context.getString(R.string.notification_channels) + suffix;
        return categoryName;
    }

    @NonNull List<NotificationChannelGroup> getNotificationChannelGroups() {
        String mipushGroup = NotificationUtils.getGroupIdByPkg(application.getPackageName());
        List<NotificationChannelGroup> groups = NotificationManagerEx.INSTANCE.getNotificationChannelGroups(application.getPackageName());
        if (NotificationManagerEx.isHooked) {
            makeMIPushGroupToTopPositions(groups, mipushGroup);
        } else {
            removeAllNonMIPushGroup(groups, mipushGroup);
        }
        return groups;
    }
}