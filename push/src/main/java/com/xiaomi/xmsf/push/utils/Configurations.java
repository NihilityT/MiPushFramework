package com.xiaomi.xmsf.push.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.xmpush.thrift.PushMetaInfo;
import com.xiaomi.xmsf.utils.ConfigCenter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import top.trumeet.common.utils.Utils;

public class Configurations {
    private static final Logger logger = XLog.tag(Configurations.class.getSimpleName()).build();

    private String version;
    private Map<String, List<Object>> packageConfigs = new HashMap<>();

    private static Configurations instance = null;
    private Context mContext = null;
    private Uri mTreeUri = null;
    private DocumentFile mDocumentFile = null;
    private long mLastLoadTime = 0;

    public static Configurations getInstance() {
        if (instance == null) {
            synchronized (Configurations.class) {
                if (instance == null) {
                    instance = new Configurations();
                }
            }
        }
        instance.reInitIfDirectoryUpdated();
        return instance;
    }


    private Configurations() {
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
    private PackageConfig parseConfig(JSONObject configObj) throws JSONException {
        PackageConfig config = new PackageConfig(this);
        if (!configObj.isNull(PackageConfig.KEY_META_INFO)) {
            config.metaInfoObj = configObj.getJSONObject(PackageConfig.KEY_META_INFO);
        }
        if (!configObj.isNull(PackageConfig.KEY_NEW_META_INFO)) {
            config.newMetaInfoObj = configObj.getJSONObject(PackageConfig.KEY_NEW_META_INFO);
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

    public Set<String> handle(String packageName, PushMetaInfo metaInfo)
            throws JSONException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        String[] checkPkgs = new String[]{"^", packageName, "$"};
        Set<String> operations = new HashSet<>();
        for (String pkg : checkPkgs) {
            List<Object> configs = packageConfigs.get(pkg);
            logger.d("package: " + packageName + ", config count: " + (configs == null ? 0 : configs.size()));
            boolean stop = doHandle(metaInfo, configs, operations);
            if (stop) {
                return operations;
            }
        }
        return operations;
    }

    private boolean doHandle(PushMetaInfo metaInfo, List<Object> configs, Set<String> operations)
            throws JSONException, NoSuchFieldException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        return doHandle(metaInfo, configs, operations, new ArrayList<>());
    }

    private boolean doHandle(PushMetaInfo metaInfo, List<Object> configs, Set<String> operations, List<Object> matched)
            throws NoSuchFieldException, IllegalAccessException, JSONException, NoSuchMethodException, InvocationTargetException {
        if (configs != null && !matched.contains(configs)) {
            matched.add(configs);
            for (Object configItem : configs) {
                if (configItem instanceof PackageConfig) {
                    PackageConfig config = (PackageConfig) configItem;
                    if (config.match(metaInfo)) {
                        config.replace(metaInfo);
                        operations.addAll(config.operation);
                        if (config.stop) {
                            return true;
                        }
                    }
                } else {
                    List<Object> refConfigs = null;
                    if (configItem instanceof JSONArray) {
                        Object value = evaluate(configItem, metaInfo);
                        if (value instanceof JSONObject) {
                            refConfigs = new ArrayList<>();
                            refConfigs.add(parseConfig((JSONObject) value));
                        } else if (value != null) {
                            refConfigs = packageConfigs.get(value.toString());
                        }
                    } else {
                        refConfigs = packageConfigs.get(configItem);
                    }
                    if (refConfigs != null) {
                        boolean stop = doHandle(metaInfo, refConfigs, operations);
                        if (stop) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean match(PushMetaInfo metaInfo, List<Object> configs, List<Object> matched)
            throws NoSuchFieldException, IllegalAccessException, JSONException {
        if (configs != null && !matched.contains(configs)) {
            matched.add(configs);
            for (Object configItem : configs) {
                if (configItem instanceof PackageConfig) {
                    PackageConfig config = (PackageConfig) configItem;
                    if (config.match(metaInfo)) {
                        return true;
                    }
                } else {
                    List<Object> refConfigs = packageConfigs.get(configItem);
                    return match(metaInfo, refConfigs, matched);
                }
            }
        }
        return false;
    }

    private void reInitIfDirectoryUpdated() {
        if (mContext == null || mTreeUri == null || mDocumentFile == null) {
            return;
        }
        if (mDocumentFile.lastModified() > mLastLoadTime) {
            init(mContext, mTreeUri);
        }
    }

    @FunctionalInterface
    public interface Callable {
        Object run();
    }

    Object evaluate(Object expr, PackageConfig env) {
        return evaluate(expr, env, null);
    }

    private Object evaluate(Object expr, PushMetaInfo env) {
        return evaluate(expr, null, env);
    }

    private Object evaluate(Object expr, PackageConfig config, PushMetaInfo metaInfo) {
        if (expr instanceof String) {
            return expr;
        }
        if (expr instanceof JSONObject) {
            return expr;
        }
        if (expr instanceof JSONArray) {
            return evaluate((JSONArray) expr, config, metaInfo);
        }
        return null;
    }

    private Object evaluate(JSONArray expr, PackageConfig config, PushMetaInfo metaInfo) {
        String method = (String) evaluate(expr.opt(0), config);
        if (method == null) {
            return null;
        }

        int length = expr.length();
        try {
            if ("cond".equals(method)) {
                for (int i = 1; i < length; ++i) {
                    JSONArray clause = expr.optJSONArray(i);
                    Object test = clause.opt(0);
                    if (test instanceof JSONObject) {
                        if (parseConfig((JSONObject) test).match(metaInfo)) {
                            return clause.opt(1);
                        }
                    }
                    if (test instanceof String) {
                        if (match(metaInfo, packageConfigs.get(test), new ArrayList<>())) {
                            return clause.opt(1);
                        }
                    }
                    if (test instanceof JSONArray) {
                        if (Boolean.TRUE.equals(evaluate(test, config, metaInfo))) {
                            Object ret = null;
                            for (int j = 1; j < clause.length(); ++j) {
                                ret = evaluate(expr.opt(j), config);
                            }
                            return ret;
                        }
                    }
                }
                return null;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }

        JSONArray evaluated = new JSONArray();
        evaluated.put(method);
        for (int i = 1; i < length; ++i) {
            evaluated.put(evaluate(expr.opt(i), config));
        }

        Map<String, Callable> methods = new HashMap<>();
        methods.put("$", () -> config.matchGroup.get(evaluated.optString(1)));
        methods.put("hash", evaluated.optString(1)::hashCode);
        methods.put("decode-uri", () -> {
            try {
                return URLDecoder.decode(evaluated.optString(1), StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
        });
        methods.put("parse-json", () -> {
            try {
                return new JSONTokener(evaluated.optString(1)).nextValue();
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        });
        methods.put("property", () -> {
            Object obj = evaluated.opt(2);
            if (obj instanceof JSONObject) {
                return ((JSONObject) obj).opt(evaluated.optString(1));
            }
            if (obj instanceof JSONArray) {
                return ((JSONArray) obj).opt(evaluated.optInt(1));
            }
            return null;
        });
        methods.put("replace", () -> {
            String src = evaluated.optString(1);
            String ptn = evaluated.optString(2);
            String rep = evaluated.optString(3);
            return src.replaceAll(ptn, rep);
        });

        Callable ret = methods.get(method);
        return ret == null ? null : ret.run();
    }
}
