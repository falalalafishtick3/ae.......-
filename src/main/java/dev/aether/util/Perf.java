package dev.aether.util;

import dev.aether.module.ClientModule;
import dev.aether.module.ModuleManager;

/** Lightweight per-module timing (only active while the PerfMonitor module is on). */
public final class Perf {
    public static volatile boolean on;
    private static final int TICK = 0, RENDER = 1, HUD = 2;
    private static long[][] sum = new long[3][0];
    private static int[] calls = new int[3];
    public static double[][] avgMs = new double[3][0];   // [kind][module] average ms per call (last second)
    private static long lastReport = System.nanoTime();

    private static void ensure() {
        int n = ModuleManager.MODULES.size();
        if (sum[0].length != n) { sum = new long[3][n]; avgMs = new double[3][n]; }
    }

    public static void add(int kind, ClientModule m, long ns) {
        ensure();
        int idx = ModuleManager.MODULES.indexOf(m);
        if (idx >= 0) sum[kind][idx] += ns;
    }
    public static void call(int kind) { calls[kind]++; }

    public static void frame() {
        long now = System.nanoTime();
        if (now - lastReport < 1_000_000_000L) return;
        ensure();
        for (int k = 0; k < 3; k++) {
            for (int i = 0; i < sum[k].length; i++) avgMs[k][i] = calls[k] == 0 ? 0 : sum[k][i] / 1e6 / calls[k];
            java.util.Arrays.fill(sum[k], 0); calls[k] = 0;
        }
        lastReport = now;
    }

    public static final int T = TICK, R = RENDER, H = HUD;
}
