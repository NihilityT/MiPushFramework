package com.xiaomi.xmsf.push.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.xmsf.utils.ConfigCenter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import top.trumeet.common.utils.Utils;

public class ConfigurationsLoader {
    private static final Logger logger = XLog.tag(ConfigurationsLoader.class.getSimpleName()).build();

    private String version;
    private Map<String, List<Object>> packageConfigs = new HashMap<>();

    private Context mContext = null;
    private Uri mTreeUri = null;
    private DocumentFile mDocumentFile = null;
    private long mLastLoadTime = 0;


    public ConfigurationsLoader() {
    }

    public Map<String, List<Object>> getConfigs() {
        return packageConfigs;
    }

    public boolean init(Context context, Uri treeUri) {
        mLastLoadTime = System.currentTimeMillis();
        packageConfigs.clear();
        do {
            if (context == null || treeUri == null) {
                break;
            }
            List<Pair<DocumentFile, JSONException>> exceptions = new ArrayList<>();
            List<DocumentFile> loadedFiles = new ArrayList<>();
            parseDirectory(context, treeUri, exceptions, loadedFiles);

            if (!loadedFiles.isEmpty() && ConfigCenter.getInstance().isShowConfigurationListOnLoaded(context)) {
                StringBuilder loadedList = new StringBuilder("loaded configuration list:");
                for (DocumentFile file : loadedFiles) {
                    loadedList.append('\n');
                    loadedList.append(file.getName());
                }
                Utils.makeText(context, loadedList, Toast.LENGTH_SHORT);
            }
            if (!exceptions.isEmpty()) {
                for (Pair<DocumentFile, JSONException> pair : exceptions) {
                    StringBuilder errmsg = getJsonExceptionMessage(context, pair);
                    logger.e(errmsg);
                    Utils.makeText(context, errmsg.toString(), Toast.LENGTH_LONG);
                }
                break;
            }
            return true;
        } while (false);
        return false;
    }

    @NonNull
    public static StringBuilder getJsonExceptionMessage(Context context, Pair<DocumentFile, JSONException> pair) {
        DocumentFile file = pair.first;
        JSONException e = pair.second;
        e.printStackTrace();

        StringBuilder errmsg = new StringBuilder(e.toString());
        Pattern pattern = Pattern.compile(" character (\\d+) of ");
        Matcher matcher = pattern.matcher(errmsg.toString());
        if (matcher.find()) {
            int pos = Integer.parseInt(matcher.group(1));
            String json = readTextFromUri(context, file.getUri());
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
        }
        errmsg.insert(0, file.getName() + "\n");
        return errmsg;
    }

    private boolean parseDirectory(Context context, Uri treeUri, List<Pair<DocumentFile, JSONException>> exceptions, List<DocumentFile> loadedFiles) {
        DocumentFile documentFile = DocumentFile.fromTreeUri(context, treeUri);
        if (documentFile == null) {
            return true;
        }
        mContext = context;
        mTreeUri = treeUri;
        mDocumentFile = documentFile;
        logger.i("parseDirectory uri: [%s]", treeUri.getPath());
        DocumentFile[] files = documentFile.listFiles();
        for (DocumentFile file : files) {
            logger.i("file: [%s], type: [%s]", file.getName(), file.getType());
            if (!file.getName().toLowerCase().endsWith(".json")) {
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
        JSONObject jsonObject = new JSONObject(json);
        version = jsonObject.getString("version");
        JSONObject packageConfigsObj = jsonObject.getJSONObject("configs");
        Iterator<String> packageNames = packageConfigsObj.keys();
        while (packageNames.hasNext()) {
            String packageName = packageNames.next();
            JSONArray configsObj = packageConfigsObj.getJSONArray(packageName);
            packageConfigs.put(packageName, parseConfigs(configsObj));
        }
    }

    @NonNull
    private ArrayList<Object> parseConfigs(JSONArray configsObj) throws JSONException {
        ArrayList<Object> configs = new ArrayList<>();
        for (int i = 0; i < configsObj.length(); ++i) {
            Object config = configsObj.get(i);
            if (config instanceof JSONArray) {
                configs.add(config);
            } else if (config instanceof String) {
                configs.add(config);
            } else {
                configs.add(parseConfig(configsObj.getJSONObject(i)));
            }
        }
        return configs;
    }

    @NonNull
    PackageConfig parseConfig(JSONObject configObj) throws JSONException {
        PackageConfig config = new PackageConfig(Configurations.getInstance());
        if (!configObj.isNull(PackageConfig.KEY_META_INFO)) {
            JSONObject obj = new JSONObject();
            obj.put(PackageConfig.KEY_META_INFO, configObj.getJSONObject(PackageConfig.KEY_META_INFO));
            config.cfgMatch = obj;
        }
        if (!configObj.isNull(PackageConfig.KEY_NEW_META_INFO)) {
            JSONObject obj = new JSONObject();
            obj.put(PackageConfig.KEY_META_INFO, configObj.getJSONObject(PackageConfig.KEY_NEW_META_INFO));
            config.cfgReplace = obj;
        }
        if (!configObj.isNull(PackageConfig.KEY_MATCH)) {
            config.cfgMatch = configObj.getJSONObject(PackageConfig.KEY_MATCH);
        }
        if (!configObj.isNull(PackageConfig.KEY_REPLACE)) {
            config.cfgReplace = configObj.getJSONObject(PackageConfig.KEY_REPLACE);;
        }
        if (!configObj.isNull(PackageConfig.KEY_OPERATION)) {
            String operations = configObj.getString(PackageConfig.KEY_OPERATION);
            config.operation = new HashSet<>(Arrays.asList(operations.split("[\\s|]+")));
        }
        if (!configObj.isNull(PackageConfig.KEY_STOP)) {
            config.stop = configObj.getBoolean(PackageConfig.KEY_STOP);
        }
        return config;
    }

    public void reInitIfDirectoryUpdated() {
        if (mContext == null || mTreeUri == null || mDocumentFile == null) {
            return;
        }
        if (mDocumentFile.lastModified() > mLastLoadTime) {
            init(mContext, mTreeUri);
        }
    }


    public static String readTextFromUri(Context context, Uri uri) {
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

}
