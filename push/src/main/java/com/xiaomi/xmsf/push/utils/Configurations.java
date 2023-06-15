package com.xiaomi.xmsf.push.utils;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.xiaomi.xmpush.thrift.XmPushActionContainer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
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
        return Lisp.evaluate(expr, new ConfigurationEvaluator(configWalker, data));
    }

    class ConfigurationEvaluator implements Lisp.Evaluable {
        PackageConfig.Walker configWalker;
        XmPushActionContainer data;

        ConfigurationEvaluator(PackageConfig.Walker configWalker, XmPushActionContainer data) {
            this.configWalker = configWalker;
            this.data = data;
        }

        @Override
        public Object evaluate(Object expr) {
            if (expr instanceof JSONObject) {
                return expr;
            }
            if (expr instanceof JSONArray) {
                return evaluate((JSONArray) expr);
            }
            return null;
        }

        private Object evaluate(JSONArray expr) {
            String method = (String) expr.opt(0);
            if (method == null) {
                return null;
            }

            if ("cond".equals(method)) {
                return evaluateCond(expr);
            }

            JSONArray evaluated = expr;
            Map<String, Callable<Object>> methods = new HashMap<>();
            methods.put("$", () -> configWalker.matchGroup.get(evaluated.optString(1)));

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

        @Nullable
        private Object evaluateCond(JSONArray expr) {
            try {
                int length = expr.length();
                for (int i = 1; i < length; ++i) {
                    JSONArray clause = expr.optJSONArray(i);
                    if (clause == null) {
                        return null;
                    }
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
                }
            } catch (Exception e) {
                logger.e("evaluateCond", e);
            }
            return null;
        }
    }
}
