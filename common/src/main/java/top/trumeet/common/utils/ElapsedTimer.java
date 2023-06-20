package top.trumeet.common.utils;

import android.os.SystemClock;

public class ElapsedTimer {
    private long start = time();

    public long start() {
        return start = time();
    }

    public long restart() {
        long old = start;
        start = time();
        return start - old;
    }

    public long elapsed() {
        return time() - start;
    }

    private long time() {
        return SystemClock.elapsedRealtime();
    }
}
