package com.nihility;

public class Dependencies {
    private static OuterDependencies outerDependencies;

    public static OuterDependencies instance() {
        return outerDependencies;
    }

    public static void set(OuterDependencies outerDependencies) {
        Dependencies.outerDependencies = outerDependencies;
    }

}

