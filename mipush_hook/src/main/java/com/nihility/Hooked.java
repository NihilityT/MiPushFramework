package com.nihility;

import java.util.HashSet;
import java.util.Set;

public class Hooked {
    private final static Set<String> hookedRecord = new HashSet<>();

    public static boolean contains(String id) {
        return hookedRecord.contains(id);
    }

    public static void mark(String id) {
        hookedRecord.add(id);
    }
}
