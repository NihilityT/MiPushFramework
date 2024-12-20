package com.nihility;

public class Dependencies {
    private Configurations configurations;

    public void init(Configurations configurations) {
        this.configurations = configurations;
    }

    public Configurations configuration() {
        return configurations;
    }

    private static class LazyHolder {
        static Dependencies INSTANCE = new Dependencies();
    }

    public static Dependencies getInstance() {
        return LazyHolder.INSTANCE;
    }
}

