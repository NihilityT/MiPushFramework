package com.xiaomi.xmsf.push.utils;

import android.os.Build;

import androidx.annotation.Nullable;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class Lisp {
    public interface Evaluable {
        Object evaluate(Object expr);
    }

    private static final Logger logger = XLog.tag(Lisp.class.getSimpleName()).build();

    public static Object evaluate(Object expr, Evaluable extension) {
        if (expr instanceof String) {
            return expr;
        }
        if (expr instanceof JSONArray) {
            return evaluate((JSONArray) expr, extension);
        }
        return extension.evaluate(expr);
    }

    private static Object evaluate(JSONArray expr, Evaluable extension) {
        String method = (String) evaluate(expr.opt(0), extension);
        if (method == null) {
            return null;
        }

        if ("cond".equals(method)) {
            return evaluateCond(expr, extension);
        }

        int length = expr.length();
        JSONArray evaluated = new JSONArray();
        evaluated.put(method);
        for (int i = 1; i < length; ++i) {
            evaluated.put(evaluate(expr.opt(i), extension));
        }

        Map<String, Callable<Object>> methods = new HashMap<>();
        methods.put("hash", evaluated.optString(1)::hashCode);
        methods.put("decode-uri", () -> URLDecoder.decode(evaluated.optString(1), StandardCharsets.UTF_8.name()));
        methods.put("decode-base64", () -> {
            String base64 = evaluated.optString(1);

            Callable<byte[]>[] decoders;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                decoders = new Callable[]{
                        () -> android.util.Base64.decode(base64, android.util.Base64.DEFAULT),
                        () -> android.util.Base64.decode(base64, android.util.Base64.URL_SAFE),
                };
            } else {
                decoders = new Callable[]{
                        () -> Base64.getDecoder().decode(base64),
                        () -> Base64.getUrlDecoder().decode(base64),
                        () -> Base64.getMimeDecoder().decode(base64),
                };
            }
            Exception err = null;
            for (Callable<byte[]> decoder : decoders) {
                try {
                    byte[] decoded = decoder.call();
                    return new String(decoded, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    err = e;
                }
            }
            throw err;
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
        if (ret == null) {
            return extension.evaluate(evaluated);
        }
        try {
            return ret.call();
        } catch (Exception e) {
            logger.e(method, e);
        }
        return null;
    }

    @Nullable
    private static Object evaluateCond(JSONArray expr, Evaluable extension) {
        int length = expr.length();
        for (int i = 1; i < length; ++i) {
            JSONArray clause = expr.optJSONArray(i);
            if (clause == null) {
                return null;
            }
            Object test = clause.opt(0);
            if (test instanceof JSONArray) {
                if (Boolean.TRUE.equals(evaluate(test, extension))) {
                    Object ret = null;
                    for (int j = 1; j < clause.length(); ++j) {
                        ret = evaluate(expr.opt(j), extension);
                    }
                    return ret;
                }
            }
            JSONArray subCond = new JSONArray();
            subCond.put("cond");
            subCond.put(clause);
            Object val = extension.evaluate(subCond);
            if (val != null) {
                return val;
            }
        }
        return null;
    }
}
