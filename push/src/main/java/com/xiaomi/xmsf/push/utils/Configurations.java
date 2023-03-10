package com.xiaomi.xmsf.push.utils;

import android.content.Context;
import android.net.Uri;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.xmpush.thrift.PushMetaInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Configurations {
    private static final Logger logger = XLog.tag(Configurations.class.getSimpleName()).build();
    private ConfigurationsLoader loader = null;
    private static Configurations instance = null;

    public static Configurations getInstance() {
        if (instance == null) {
            synchronized (Configurations.class) {
                if (instance == null) {
                    instance = new Configurations();
                }
            }
        }
        return instance;
    }


    private Configurations() {
    }

    public boolean init(Context context, Uri treeUri) {
        loader = new ConfigurationsLoader();
        return loader.init(context, treeUri);
    }

    public Set<String> handle(String packageName, PushMetaInfo metaInfo)
            throws JSONException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        String[] checkPkgs = new String[]{"^", packageName, "$"};
        Set<String> operations = new HashSet<>();
        for (String pkg : checkPkgs) {
            List<Object> configs = loader.getConfigs().get(pkg);
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
                            refConfigs.add(loader.parseConfig((JSONObject) value));
                        } else if (value != null) {
                            refConfigs = loader.getConfigs().get(value.toString());
                        }
                    } else {
                        refConfigs = loader.getConfigs().get(configItem);
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
                    List<Object> refConfigs = loader.getConfigs().get(configItem);
                    return match(metaInfo, refConfigs, matched);
                }
            }
        }
        return false;
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
                        if (loader.parseConfig((JSONObject) test).match(metaInfo)) {
                            return clause.opt(1);
                        }
                    }
                    if (test instanceof String) {
                        if (match(metaInfo, loader.getConfigs().get(test), new ArrayList<>())) {
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
