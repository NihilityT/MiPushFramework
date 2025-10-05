package com.nihility.utils;

import androidx.annotation.NonNull;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Singleton {
    static Map<Class<?>, Object> instances = new ConcurrentHashMap<>();
    static Map<Class<?>, Object> userInstances = new ConcurrentHashMap<>();

    public static <T> T instance(T... reified) {
        if (reified.length > 0) {
            throw new IllegalArgumentException(
                    "Please don't pass any values here. Java will detect class automagically.");
        }
        Class<T> klass = getClassOf(reified);

        T obj = (T) userInstances.get(klass);
        if (obj != null) {
            return obj;
        }

        obj = (T) instances.get(klass);
        if (obj != null) {
            return obj;
        }

        return create(klass);
    }

    private static synchronized <T> @NonNull T create(Class<T> klass) {
        T obj = (T) instances.get(klass);
        if (obj != null) {
            return obj;
        }
        try {
            Constructor<T> constructor = klass.getDeclaredConstructor();
            constructor.setAccessible(true);
            obj = constructor.newInstance();
            instances.put(klass, obj);
            return obj;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> AutoReset reset(T... reified) {
        Class<T> klass = getClassOf(reified);

        if (reified.length == 0) {
            userInstances.remove(klass);
        } else {
            userInstances.put(klass, reified[0]);
        }
        return new AutoReset(klass);
    }

    public static class AutoReset implements AutoCloseable {
        Class<?> klass;
        AutoReset(Class<?> klass) {
            this.klass = klass;
        }

        @Override
        public void close() {
            Singleton.userInstances.remove(klass);
        }
    }

    private static <T> Class<T> getClassOf(T[] array) {
        return (Class<T>) array.getClass().getComponentType();
    }
}
