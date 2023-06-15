package com.xiaomi.xmsf.push.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class Configurations {
    private static final Logger logger = XLog.tag(Configurations.class.getSimpleName()).build();
    private ConfigurationsLoader loader = new ConfigurationsLoader();
    private static Configurations instance = null;

    public static Configurations getInstance() {
        if (instance == null) {
            synchronized (Configurations.class) {
                if (instance == null) {
                    instance = new Configurations();
                }
            }
        }
        instance.loader.reInitIfDirectoryUpdated();
        return instance;
    }


    private Configurations() {
    }

    public boolean init(Context context, Uri treeUri) {
        return loader.init(context, treeUri);
    }

    public Set<String> handle(String packageName, XmPushActionContainer data)
            throws JSONException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        String[] checkPkgs = new String[]{"^", packageName, "$"};
        Set<String> operations = new HashSet<>();
        for (String pkg : checkPkgs) {
            List<Object> configs = loader.getConfigs().get(pkg);
            logger.d("package: " + packageName + ", config count: " + (configs == null ? 0 : configs.size()));
            boolean stop = doHandle(data, configs, operations);
            if (stop) {
                return operations;
            }
        }
        return operations;
    }

    private boolean doHandle(XmPushActionContainer data, List<Object> configs, Set<String> operations)
            throws JSONException, NoSuchFieldException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        return doHandle(data, configs, operations, new ArrayList<>());
    }

    private boolean doHandle(XmPushActionContainer data, List<Object> configs, Set<String> operations, List<Object> matched)
            throws NoSuchFieldException, IllegalAccessException, JSONException, NoSuchMethodException, InvocationTargetException {
        if (configs != null && !matched.contains(configs)) {
            matched.add(configs);
            for (Object configItem : configs) {
                if (configItem instanceof PackageConfig) {
                    PackageConfig config = (PackageConfig) configItem;
                    PackageConfig.Walker walker = config.getWalker(data);
                    if (walker.match()) {
                        walker.replace();
                        operations.addAll(config.operation);
                        if (config.stop) {
                            return true;
                        }
                    }
                } else {
                    List<Object> refConfigs = null;
                    if (configItem instanceof JSONArray) {
                        Object value = evaluate(configItem, data);
                        if (value instanceof JSONObject) {
                            refConfigs = new ArrayList<>();
                            refConfigs.add(loader.parseConfig((JSONObject) value));
                        } else if (value != null) {
                            refConfigs = loader.getConfigs().get(value.toString());
                        }
                    } else {
                        refConfigs = loader.getConfigs().get(configItem);
                    }
                    if (refConfigs != null) {
                        boolean stop = doHandle(data, refConfigs, operations);
                        if (stop) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean match(XmPushActionContainer data, List<Object> configs, List<Object> matched)
            throws NoSuchFieldException, IllegalAccessException {
        if (configs != null && !matched.contains(configs)) {
            matched.add(configs);
            for (Object configItem : configs) {
                if (configItem instanceof PackageConfig) {
                    PackageConfig config = (PackageConfig) configItem;
                    PackageConfig.Walker walker = config.getWalker(data);
                    if (walker.match()) {
                        return true;
                    }
                } else {
                    List<Object> refConfigs = loader.getConfigs().get(configItem);
                    return match(data, refConfigs, matched);
                }
            }
        }
        return false;
    }

    Object evaluate(Object expr, PackageConfig.Walker env) {
        return evaluate(expr, env, null);
    }

    private Object evaluate(Object expr, XmPushActionContainer env) {
        return evaluate(expr, null, env);
    }

    private Object evaluate(Object expr, PackageConfig.Walker configWalker, XmPushActionContainer data) {
        if (expr instanceof String) {
            return expr;
        }
        if (expr instanceof JSONObject) {
            return expr;
        }
        if (expr instanceof JSONArray) {
            return evaluate((JSONArray) expr, configWalker, data);
        }
        return null;
    }

    private Object evaluate(JSONArray expr, PackageConfig.Walker configWalker, XmPushActionContainer data) {
        String method = (String) evaluate(expr.opt(0), configWalker);
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
                        PackageConfig config = loader.parseConfig((JSONObject) test);
                        if (config.getWalker(data).match()) {
                            return clause.opt(1);
                        }
                    }
                    if (test instanceof String) {
                        if (match(data, loader.getConfigs().get(test), new ArrayList<>())) {
                            return clause.opt(1);
                        }
                    }
                    if (test instanceof JSONArray) {
                        if (Boolean.TRUE.equals(evaluate(test, configWalker, data))) {
                            Object ret = null;
                            for (int j = 1; j < clause.length(); ++j) {
                                ret = evaluate(expr.opt(j), configWalker);
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
            evaluated.put(evaluate(expr.opt(i), configWalker));
        }

        Map<String, Callable<Object>> methods = new HashMap<>();
        methods.put("$", () -> configWalker.matchGroup.get(evaluated.optString(1)));
        methods.put("hash", evaluated.optString(1)::hashCode);
        methods.put("decode-uri", () -> URLDecoder.decode(evaluated.optString(1), StandardCharsets.UTF_8.name()));
        methods.put("decode-base64", () -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return null;
            }
            Base64.Decoder decoder;
            switch (evaluated.optString(2)) {
                case "url":
                    decoder = Base64.getUrlDecoder();
                    break;
                case "mime":
                    decoder = Base64.getMimeDecoder();
                    break;
                default:
                    decoder = Base64.getDecoder();
                    break;
            }
            return new String(decoder.decode(evaluated.optString(1)), StandardCharsets.UTF_8);
        });
        methods.put("parse-json", () -> new JSONTokener(evaluated.optString(1)).nextValue());
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

        Callable<Object> ret = methods.get(method);
        if (ret != null) {
            try {
                return ret.call();
            } catch (Exception e) {
                logger.e(method, e);
            }
        }
        return null;
    }
}
