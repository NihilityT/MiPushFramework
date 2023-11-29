package top.trumeet.common.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CustomConfiguration {
    private static String Config(String name) {
        return "__mi_push_" + name;
    }

    private static final String SUB_TEXT = Config("sub_text");
    private static final String ROUND_LARGE_ICON = Config("round_large_icon");
    private static final String USE_MESSAGING_STYLE = Config("use_messaging_style");
    private static final String CONVERSATION_TITLE = Config("conversation_title");
    private static final String CONVERSATION_ID = Config("conversation_id");
    private static final String CONVERSATION_ICON = Config("conversation_icon");
    private static final String CONVERSATION_IMPORTANT = Config("conversation_important");
    private static final String CONVERSATION_SENDER = Config("conversation_sender");
    private static final String CONVERSATION_SENDER_ID = Config("conversation_sender_id");
    private static final String CONVERSATION_SENDER_ICON = Config("conversation_sender_icon");
    private static final String CONVERSATION_MESSAGE = Config("conversation_message");
    private static final String CLEAR_GROUP = Config("clear_group");
    private static final String BORROW_CHANNEL_ID = Config("borrow_channel_id");

    private static final String NOTIFICATION_LARGE_ICON_URI = "notification_large_icon_uri";
    private static final String CHANNEL_ID = "channel_id";
    private static final String CHANNEL_NAME = "channel_name";
    private static final String CHANNEL_DESCRIPTION = "channel_description";
    private static final String SOUND_URL = "sound_url";
    private static final String JOBKEY = "jobkey";
    private static final String USE_CLICKED_ACTIVITY = "use_clicked_activity";
    private static final String NOTIFICATION_GROUP = "notification_group";
    private static final String NOTIFICATION_BIGPIC_URI = "notification_bigPic_uri";
    private static final String FOCUS_PARAM = "miui.focus.param";

    private Map<String, String> mExtra = new HashMap<>();

    public CustomConfiguration(Map<String, String> extra) {
        if (extra != null) {
            mExtra = extra;
        }
    }

    public String subText(String defaultValue) {
        return get(SUB_TEXT, defaultValue);
    }

    public boolean roundLargeIcon(boolean defaultValue) {
        return get(ROUND_LARGE_ICON, defaultValue);
    }

    public boolean useMessagingStyle(boolean defaultValue) {
        return get(USE_MESSAGING_STYLE, defaultValue);
    }

    public String conversationTitle(String defaultValue) {
        return get(CONVERSATION_TITLE, defaultValue);
    }

    public String conversationId(String defaultValue) {
        return get(CONVERSATION_ID, defaultValue);
    }

    public String conversationIcon(String defaultValue) {
        return get(CONVERSATION_ICON, defaultValue);
    }

    public boolean conversationImportant(boolean defaultValue) {
        return get(CONVERSATION_IMPORTANT, defaultValue);
    }

    public String conversationSender(String defaultValue) {
        return get(CONVERSATION_SENDER, defaultValue);
    }

    public String conversationSenderId(String defaultValue) {
        return get(CONVERSATION_SENDER_ID, defaultValue);
    }

    public String conversationSenderIcon(String defaultValue) {
        return get(CONVERSATION_SENDER_ICON, defaultValue);
    }

    public String conversationMessage(String defaultValue) {
        return get(CONVERSATION_MESSAGE, defaultValue);
    }

    public String notificationLargeIconUri(String defaultValue) {
        return get(NOTIFICATION_LARGE_ICON_URI, defaultValue);
    }

    public String channelId(String defaultValue) {
        return get(CHANNEL_ID, defaultValue);
    }

    public String channelName(String defaultValue) {
        return get(CHANNEL_NAME, defaultValue);
    }

    public String channelDescription(String defaultValue) {
        return get(CHANNEL_DESCRIPTION, defaultValue);
    }

    public String soundUrl(String defaultValue) {
        return get(SOUND_URL, defaultValue);
    }

    public String jobkey(String defaultValue) {
        return get(JOBKEY, defaultValue);
    }

    public boolean useClickedActivity(boolean defaultValue) {
        return get(USE_CLICKED_ACTIVITY, defaultValue);
    }

    public String notificationGroup(String defaultValue) {
        return get(NOTIFICATION_GROUP, defaultValue);
    }

    public String notificationBigPicUri(String defaultValue) {
        return get(NOTIFICATION_BIGPIC_URI, defaultValue);
    }

    public boolean clearGroup(boolean defaultValue) {
        return get(CLEAR_GROUP, defaultValue);
    }
    public String borrowChannelId(String defaultValue) {
        return get(BORROW_CHANNEL_ID, defaultValue);
    }

    public String focusParam(String defaultValue) {
        return get(FOCUS_PARAM, defaultValue);
    }

    public boolean get(String key, boolean defaultValue) {
        if (getExtraField(mExtra, key, null) != null) {
            return true;
        }
        return defaultValue;
    }

    public String get(String key, String defaultValue) {
        return getExtraField(mExtra, key, defaultValue);
    }

    public Set<String> keys() {
        if (mExtra == null) {
            return new HashSet<>();
        }
        return mExtra.keySet();
    }

    private static String getExtraField(Map<String, String> extra, String extraChannelName, String defaultValue) {
        return extra != null && extra.containsKey(extraChannelName) ?
                extra.get(extraChannelName) : defaultValue;
    }
}
