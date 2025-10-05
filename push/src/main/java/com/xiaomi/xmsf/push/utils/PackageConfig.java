package com.xiaomi.xmsf.push.utils;

import android.os.Build;

import androidx.annotation.NonNull;

import com.nihility.Global;

import org.apache.thrift.TBase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackageConfig {
    public static final String KEY_MATCH = "match";
    public static final String KEY_REPLACE = "replace";
    public static final String KEY_META_INFO = "metaInfo";
    public static final String KEY_NEW_META_INFO = "newMetaInfo";
    public static final String KEY_OPERATION = "operation";
    public static final String KEY_STOP = "stop";

    public static final String OPERATION_OPEN = "open";
    public static final String OPERATION_IGNORE = "ignore";
    public static final String OPERATION_NOTIFY = "notify";
    public static final String OPERATION_WAKE = "wake";

    private Configurations configurations;

    JSONObject cfgMatch;
    JSONObject cfgReplace;
    Set<String> operation = new HashSet<>();
    boolean stop = true;

    public PackageConfig(Configurations configurations) {
        this.configurations = configurations;
    }

    public Walker getWalker(TBase data) { return new Walker(data); }

    class Walker {
        Map<String, String> matchGroup;
        TBase data;

        Walker(TBase data) {
            this.data = data;
        }

        public boolean match() throws NoSuchFieldException, IllegalAccessException {
            matchGroup = PackageConfig.match(data, cfgMatch);
            return matchGroup != null;
        }

        public void replace() throws NoSuchFieldException, IllegalAccessException {
            if (matchGroup != null) {
                PackageConfig.replace(data, cfgReplace, configurations, this);
            }
        }

        @NonNull
        private String replace(String value) {
            Pattern pattern = Pattern.compile("\\${2}|\\$\\{([^}]+)\\}");
            Matcher matcher = pattern.matcher(value);
            StringBuilder sb = new StringBuilder(value);
            class Pair {
                int start;
                int end;
                String str = null;
            }
            List<Pair> pairs = new ArrayList<>();
            while (matcher.find()) {
                Pair pair = new Pair();
                pair.start = matcher.start();
                pair.end = matcher.end();
                if (matcher.groupCount() == 0) {
                    pair.str = "$";
                } else {
                    String groupName = matcher.group(1);
                    if (matchGroup.containsKey(groupName)) {
                        pair.str = matchGroup.get(groupName);
                    }
                }
                if (pair.str != null) {
                    pairs.add(pair);
                }
            }

            for (int i = pairs.size() - 1; i >= 0; --i) {
                Pair pair = pairs.get(i);
                sb.replace(pair.start, pair.end, pair.str);
            }
            return sb.toString();
        }
    }

    private static Map<String, String> match(TBase data, JSONObject cfgMatch)
            throws NoSuchFieldException, IllegalAccessException {
        return match(data, data, cfgMatch, new String[]{});
    }

    private static Map<String, String> match(TBase root, TBase data, JSONObject cfgMatch, String[] path)
            throws NoSuchFieldException, IllegalAccessException {
        Map<String, String> matchGroup = new HashMap<>();
        if (cfgMatch == null) {
            return matchGroup;
        }
        Iterator<String> cfgKeys = cfgMatch.keys();
        while (cfgKeys.hasNext()) {
            String cfgKey = cfgKeys.next();
            final Field field = data.getClass().getDeclaredField(cfgKey);
            String[] newPath = concat(path, new String[]{cfgKey});
            Object value = Global.ConfigValueConverter().convert(root, newPath, field.get(data));

            boolean isMap = value instanceof Map;
            boolean isTBase = value instanceof TBase;

            JSONObject cfgSubObj = null;
            if (isMap || isTBase) {
                try {
                    cfgSubObj = cfgMatch.getJSONObject(cfgKey);
                } catch (JSONException e) {
                    throw new NoSuchFieldException(
                            String.format("The type of field \"%s\" is %s, not %s", cfgKey,
                                    value.getClass().getSimpleName(), cfgMatch.opt(cfgKey).getClass()));
                }
            }

            if (isMap) {
                Map subMap = (Map) value;

                Iterator<String> cfgSubKeys = cfgSubObj.keys();
                while (cfgSubKeys.hasNext()) {
                    String cfgSubKey = cfgSubKeys.next();
                    String[] subPath = concat(newPath, new String[]{cfgSubKey});
                    if (mismatchField(cfgSubObj, cfgSubKey,
                            Global.ConfigValueConverter().convert(root, subPath, subMap.get(cfgSubKey)),
                            matchGroup)) {
                        return null;
                    }
                }
            } else if (isTBase) {
                Map<String, String> group = match(root, (TBase) value, cfgSubObj, newPath);
                if (group == null) {
                    return null;
                }
                matchGroup.putAll(group);
            } else {
                if (mismatchField(cfgMatch, cfgKey, value, matchGroup)) {
                    return null;
                }
            }
        }
        return matchGroup;
    }

    private static void replace(TBase data, JSONObject cfgReplace, Configurations configurations, PackageConfig.Walker configWalker)
            throws NoSuchFieldException, IllegalAccessException {
        if (cfgReplace == null) {
            return;
        }
        Iterator<String> cfgKeys = cfgReplace.keys();
        while (cfgKeys.hasNext()) {
            String cfgKey = cfgKeys.next();
            final Field field = data.getClass().getDeclaredField(cfgKey);

            boolean isMap = Map.class.isAssignableFrom(field.getType());
            boolean isTBase = TBase.class.isAssignableFrom(field.getType());

            JSONObject cfgSubObj = null;
            if (isMap || isTBase) {
                try {
                    cfgSubObj = cfgReplace.getJSONObject(cfgKey);
                } catch (JSONException e) {
                    throw new NoSuchFieldException(
                            String.format("The type of field \"%s\" is %s, not %s", cfgKey,
                                    field.getType().getSimpleName(), cfgReplace.opt(cfgKey).getClass()));
                }
            }

            if (isMap) {
                Map subMap = (Map) field.get(data);

                Iterator<String> cfgSubKeys = cfgSubObj.keys();
                while (cfgSubKeys.hasNext()) {
                    String cfgSubKey = cfgSubKeys.next();
                    if (cfgSubObj.isNull(cfgSubKey)) {
                        subMap.remove(cfgSubKey);
                    } else {
                        Object cfgSubVal = cfgSubObj.opt(cfgSubKey);
                        if (cfgSubVal instanceof JSONArray) {
                            Object value = configurations.evaluate(cfgSubVal, configWalker);
                            if (value == null) {
                                subMap.remove(cfgSubKey);
                            } else {
                                subMap.put(cfgSubKey, value.toString());
                            }
                        } else {
                            String cfgValue = cfgSubObj.optString(cfgSubKey);
                            cfgValue = configWalker.replace(cfgValue);
                            subMap.put(cfgSubKey, cfgValue);
                        }
                    }
                }
            } else if (isTBase) {
                replace((TBase) field.get(data), cfgSubObj, configurations, configWalker);
            } else {
                Object cfgValueObj = cfgReplace.opt(cfgKey);
                boolean evaluated = cfgValueObj instanceof JSONArray;
                if (evaluated) {
                    cfgValueObj = configurations.evaluate(cfgValueObj, configWalker);
                }
                if (cfgReplace.isNull(cfgKey) || cfgValueObj == null) {
                    try {
                        String capitalizedKey = cfgKey.substring(0, 1).toUpperCase() + cfgKey.substring(1);
                        final Method method;
                        method = data.getClass().getDeclaredMethod("unset" + capitalizedKey);
                        method.invoke(data);
                    } catch (NoSuchMethodException | InvocationTargetException e) {
                        // Ignore
                    }
                } else {
                    String value = cfgValueObj.toString();
                    if (!evaluated) {
                        value = configWalker.replace(value);
                    }
                    Object typedValue = null;
                    final Class<?> fieldType = field.getType();
                    if (long.class == fieldType) {
                        typedValue = Long.parseLong(value);
                    } else if (int.class == fieldType) {
                        typedValue = (int) Long.parseLong(value);
                    } else if (boolean.class == fieldType) {
                        typedValue = Boolean.parseBoolean(value);
                    } else {
                        // Assume String
                        typedValue = value;
                    }
                    field.set(data, typedValue);
                }
            }
        }
    }

    private static boolean mismatchField(JSONObject obj, String cfgKey, Object value, Map<String, String> matchGroup) {
        if (obj.isNull(cfgKey)) {
            return value != null;
        } else if (value == null) {
            return true;
        }

        String regex = obj.optString(cfgKey);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(value.toString());
        if (!matcher.find()) {
            return true;
        }
        List<String> groups = getNamedGroupCandidates(regex);
        for (int i = 0; i < groups.size(); ++i) {
            String name = groups.get(i);
            matchGroup.put(name,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                            matcher.group(name) :
                            matcher.group(i + 1));
        }
        return false;
    }

    private static ArrayList<String> getNamedGroupCandidates(String regex) {
        ArrayList<String> namedGroups = new ArrayList<>();
        Matcher m = Pattern.compile("(?<!\\\\)\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>").matcher(regex);
        while (m.find()) {
            namedGroups.add(m.group(1));
        }
        return namedGroups;
    }

    public static String[] concat(String[] first, String[] second) {
        String[] result = new String[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
