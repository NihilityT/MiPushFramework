package top.trumeet.common.utils;

import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.widget.Toast;
import android.util.Base64;

import androidx.documentfile.provider.DocumentFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomNotifyIcon {
    // private static final Logger logger = XLog.tag(CustomNotifyIcon.class.getSimpleName()).build();

    public class CustomNotifyIconItem {
        public static final String KEY_APP_NAME = "appName";
        public static final String KEY_PACKAGE_NAME = "packageName";
        public static final String KEY_ICON_BITMAP = "iconBitmap";
        public static final String KEY_ICON_COLOR = "iconColor";

        private String packageName;
        private String iconBitmapBase64;
        private String iconColor;

        public CustomNotifyIconItem(String packageName, JSONObject jObj) throws JSONException {
            this.packageName = packageName;
            iconBitmapBase64 = jObj.getString(KEY_ICON_BITMAP);
            try {
                iconColor = jObj.getString(KEY_ICON_COLOR);
            } catch (Throwable ignore) {
                iconColor = "#FFFFFFFF";
            }

        }
    }

    private Map<String, Object> notifyIcons = new HashMap<>();

    private static CustomNotifyIcon instance = null;

    public static CustomNotifyIcon getInstance() {
        if (instance == null) {
            synchronized (CustomNotifyIcon.class) {
                if (instance == null) {
                    instance = new CustomNotifyIcon();
                }
            }
        }
        return instance;
    }

    private CustomNotifyIcon() {
    }

    public int tryGetIconColor(String pkg) {
        Object iconItem = notifyIcons.get(pkg);
        if (iconItem != null && iconItem instanceof CustomNotifyIconItem) {
            return Color.parseColor(((CustomNotifyIconItem) iconItem).iconColor);
        }
        return -1;
    }

    public Bitmap tryGetIconBitmap(String pkg) {
        Object iconItem = notifyIcons.get(pkg);
        if (iconItem != null && iconItem instanceof CustomNotifyIconItem) {
            try {
                byte[] bitmapArray = Base64.decode(((CustomNotifyIconItem) iconItem).iconBitmapBase64, Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public boolean init(Context context, Uri treeUri) {
        notifyIcons = new HashMap<>();
        do {
            if (context == null || treeUri == null) {
                break;
            }
            DocumentFile documentFile = DocumentFile.fromSingleUri(context, treeUri);
            if (documentFile != null && "application/json".equals(documentFile.getType())) {
                String json = readTextFromUri(context, documentFile.getUri());
                try {
                    parse(json);
                    StringBuilder loadedList = new StringBuilder("loaded custom notify icons:");
                    loadedList.append(documentFile.getName());
                    Utils.makeText(context, loadedList, Toast.LENGTH_SHORT);
                } catch (JSONException e) {
                    e.printStackTrace();

                    StringBuilder errmsg = new StringBuilder(e.toString());
                    Pattern pattern = Pattern.compile(" character (\\d+) of ");
                    Matcher matcher = pattern.matcher(errmsg.toString());
                    if (matcher.find()) {
                        int pos = Integer.parseInt(matcher.group(1));
                        String[] beforeErr = json.substring(0, pos).split("\n");
                        int errorLine = beforeErr.length;
                        int errorColumn = beforeErr[beforeErr.length - 1].length();
                        String exceptionMessage = errmsg.substring(0, matcher.start())
                                .replace("org.json.JSONException: ", "")
                                .replaceFirst("(after )(.*)( at)", "$1\"$2\"$3");
                        errmsg = new StringBuilder(String.format("%s line %d column %d", exceptionMessage, errorLine, errorColumn));

                        String[] jsonLine = json.split("\n");
                        jsonLine[errorLine - 1] = jsonLine[errorLine - 1].substring(0, errorColumn - 1) +
                                "┋" +
                                jsonLine[errorLine - 1].substring(errorColumn - 1);
                        for (int i = Math.max(0, errorLine - 2); i <= Math.min(jsonLine.length - 1, errorLine); ++i) {
                            errmsg.append('\n');
                            errmsg.append(i + 1);
                            errmsg.append(": ");
                            errmsg.append(jsonLine[i]);
                        }
                        errmsg.insert(0, documentFile.getName() + "\n");
                        Utils.makeText(context, errmsg.toString(), Toast.LENGTH_LONG);
                        break;
                    }

                }
            }
            return true;
        } while (false);
        return false;
    }

    private String readTextFromUri(Context context, Uri uri) {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            char[] buffer = new char[1024];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                stringBuilder.append(buffer, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.makeText(context, e.toString(), Toast.LENGTH_LONG);
        }
        return stringBuilder.toString();
    }

    private void parse(String json) throws JSONException {
        JSONArray icons = new JSONArray(json);
        for (int i = 0; i < icons.length(); i++) {
            JSONObject iconObj = icons.getJSONObject(i);
            String packageName = iconObj.getString(CustomNotifyIconItem.KEY_PACKAGE_NAME);
            notifyIcons.put(packageName, new CustomNotifyIconItem(packageName, iconObj));
        }
    }
}
