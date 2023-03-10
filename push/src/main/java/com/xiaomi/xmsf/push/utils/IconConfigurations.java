package com.xiaomi.xmsf.push.utils;

import static com.xiaomi.xmsf.push.utils.ConfigurationsLoader.getJsonExceptionMessage;
import static com.xiaomi.xmsf.push.utils.ConfigurationsLoader.readTextFromUri;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.util.Base64;
import android.util.Pair;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.documentfile.provider.DocumentFile;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.xiaomi.xmsf.utils.ConfigCenter;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import top.trumeet.common.utils.Utils;

public class IconConfigurations {
    private static final Logger logger = XLog.tag(IconConfigurations.class.getSimpleName()).build();

    public class IconConfig {
        public String appName;
        public String packageName;
        public String iconBitmap;
        public String iconColor;
        public String contributorName;
        public Boolean isEnabled;
        public Boolean isEnabledAll;

        public Bitmap bitmap() {
            try {
                byte[] bitmapArray = Base64.decode(iconBitmap, Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);
            } catch (Throwable ignored) {
                return null;
            }
        }

        public int color() {
            if (iconColor == null) {
                return NotificationCompat.COLOR_DEFAULT;
            }
            return Color.parseColor(iconColor);
        }
    }

    private Map<String, IconConfig> iconConfigs = new HashMap<>();

    private static IconConfigurations instance = null;

    public static IconConfigurations getInstance() {
        if (instance == null) {
            synchronized (IconConfigurations.class) {
                if (instance == null) {
                    instance = new IconConfigurations();
                }
            }
        }
        return instance;
    }

    private IconConfigurations() {
    }

    public boolean init(Context context, Uri treeUri) {
        iconConfigs.clear();
        do {
            if (context == null || treeUri == null) {
                break;
            }
            List<Pair<DocumentFile, JSONException>> exceptions = new ArrayList<>();
            List<DocumentFile> loadedFiles = new ArrayList<>();
            parseDirectory(context, treeUri, exceptions, loadedFiles);

            if (!loadedFiles.isEmpty() && ConfigCenter.getInstance().isShowConfigurationListOnLoaded(context)) {
                StringBuilder loadedList = new StringBuilder("loaded icon configuration list:");
                for (DocumentFile file : loadedFiles) {
                    loadedList.append('\n');
                    loadedList.append(file.getName());
                }
                Utils.makeText(context, loadedList, Toast.LENGTH_SHORT);
            }
            if (!exceptions.isEmpty()) {
                for (Pair<DocumentFile, JSONException> pair : exceptions) {
                    StringBuilder errmsg = getJsonExceptionMessage(context, pair);
                    Utils.makeText(context, errmsg.toString(), Toast.LENGTH_LONG);
                }
                break;
            }
            return true;
        } while (false);
        return false;
    }

    public IconConfig get(String pkg) {
        return iconConfigs.get(pkg);
    }

    private boolean parseDirectory(Context context, Uri treeUri, List<Pair<DocumentFile, JSONException>> exceptions, List<DocumentFile> loadedFiles) {
        DocumentFile documentFile = DocumentFile.fromTreeUri(context, treeUri);
        if (documentFile == null) {
            return true;
        }
        documentFile = documentFile.findFile("icon");
        if (documentFile == null) {
            return true;
        }
        DocumentFile[] files = documentFile.listFiles();
        for (DocumentFile file : files) {
            if (!"application/json".equals(file.getType())) {
                continue;
            }
            String json = readTextFromUri(context, file.getUri());
            try {
                parse(json);
                loadedFiles.add(file);
            } catch (JSONException e) {
                exceptions.add(new Pair<>(file, e));
            }
        }
        return false;
    }

    private void parse(String json) throws JSONException {
        new JSONArray(json); // check syntax
        IconConfig[] configs = new Gson().fromJson(json, IconConfig[].class);
        for (IconConfig config : configs) {
            iconConfigs.put(config.packageName, config);
        }
    }

}
