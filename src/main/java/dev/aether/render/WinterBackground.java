package dev.aether.render;

import dev.aether.module.impl.ClickGuiModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animated winter scene. Built from a small number of fill calls (see quality): sky, stars, aurora, snow, hills,
 * plus moon / mountains / pines when scenic (main menu). In the ClickGUI (non-scenic) quality is capped at Medium.
 */
public final class WinterBackground {
    private static final class Flake { float x, y, vy, drift, size, phase; int layer; }
    private static final List<Flake> FLAKES = new ArrayList<>();
    private static final Random RNG = new Random();
    private static long lastNs = System.nanoTime();
    private static int lastW = -1, lastH = -1;

    private static Flake newFlake(int w, int h, boolean anywhere) {
        Flake f = new Flake();
        f.layer = RNG.nextInt(3);
        f.x = RNG.nextFloat() * w;
        f.y = anywhere ? RNG.nextFloat() * h : -6 - RNG.nextFloat() * 20;
        f.vy = 14f + f.layer * 12f + RNG.nextFloat() * 8f;
        f.drift = 6f + RNG.nextFloat() * 10f;
        f.size = 1 + f.layer;
        f.phase = RNG.nextFloat() * 6.28f;
        return f;
    }

    private static double hill(int x, double a) { return 14 + 9 * Math.sin(x * 0.011 + a) + 6 * Math.sin(x * 0.027) + 4 * Math.sin(x * 0.06 + 2); }

    public static void draw(DrawContext g, int w, int h, boolean scenic) {
        long nowNs = System.nanoTime();
        float dt = Math.min(0.1f, (nowNs - lastNs) / 1e9f);
        lastNs = nowNs;
        double t = Util.getMeasuringTimeMs() / 1000.0;
        if (w != lastW || h != lastH) { FLAKES.clear(); lastW = w; lastH = h; }
        int q = ClickGuiModule.quality();
        if (!scenic) q = Math.max(q, 1);

        // sky
        if (scenic) g.fillGradient(0, 0, w, h, 0xFF07122A, 0xFF21496F);
        else g.fillGradient(0, 0, w, h, 0x6A0B1F3A, 0x8A1C3F63);

        // stars
        int stars = q == 0 ? (scenic ? 80 : 35) : q == 1 ? (scenic ? 35 : 18) : 10;
        for (int i = 0; i < stars; i++) {
            int sx = (int) ((i * 97L * 31 % 1000) / 1000.0 * w);
            int sy = (int) ((i * 53L * 17 % 1000) / 1000.0 * h * 0.55);
            float tw = (float) (0.5 + 0.5 * Math.sin(t * 1.5 + i));
            g.fill(sx, sy, sx + 1, sy + 1, ((int) (60 + 150 * tw) << 24) | 0xDDEEFF);
        }

        // moon (3 soft glow squares + disc)
        if (scenic) {
            int mx = (int) (w * 0.80), my = (int) (h * 0.20), r = Math.max(9, h / 22);
            for (int k = 3; k >= 1; k--) {
                int rr = r + k * 9;
                dev.aether.gui.Ui.circle(g, mx, my, rr, (7 << 24) | 0xBFD9FF);
            }
            dev.aether.gui.Ui.circle(g, mx, my, r, 0xFFF4F8FF);
            g.fill(mx - r / 3 - 2, my - r / 3, mx - r / 3 + 2, my - r / 3 + 3, 0xFFD6E2F2);
        }

        // aurora curtains (coarse strips)
        if (ClickGuiModule.aurora() && q < 2) {
            int step = q == 0 ? 6 : 16, mid = (int) (h * 0.28), bot = (int) (h * 0.62);
            for (int x = 0; x < w; x += step) {
                double w1 = Math.sin(x * 0.012 + t * 0.55), w2 = Math.sin(x * 0.027 - t * 0.8), w3 = Math.sin(x * 0.005 + t * 0.2);
                double v = 0.5 + 0.5 * (0.55 * w1 + 0.3 * w2 + 0.15 * w3);
                int a = (int) ((scenic ? 90 : 70) * v * v);
                if (a < 4) continue;
                float hue = ClickGuiModule.baseHue() - 0.04f + 0.16f * (float) (0.5 + 0.5 * Math.sin(x * 0.006 + t * 0.3));
                int rgb = MathHelper.hsvToRgb(hue, 0.65f, 1f) & 0xFFFFFF;
                int y0 = (int) (mid - 40 + 25 * w1), y1 = (int) (bot - 40 + 30 * w2);
                g.fillGradient(x, y0 / 2, x + step, y0 + (y1 - y0) / 3, rgb, (a << 24) | rgb);
                g.fillGradient(x, y0 + (y1 - y0) / 3, x + step, y1, (a << 24) | rgb, rgb);
            }
        }

        // mountains
        if (scenic) {
            int mstep = q == 0 ? 4 : q == 1 ? 12 : 20;
            for (int layer = 0; layer < 2; layer++) {
                int top0 = layer == 0 ? 0xFF2C5680 : 0xFF183654;
                for (int x = 0; x < w; x += mstep) {
                    double m = h * (layer == 0 ? 0.34 : 0.24) + 26 * Math.sin(x * 0.006 + 0.5 + layer * 2) + 15 * Math.sin(x * 0.017 + 2 + layer) + 7 * Math.sin(x * 0.04);
                    int top = h - (int) m;
                    g.fill(x, top, x + mstep, h, top0);
                    g.fill(x, top, x + mstep, top + 4, 0xFFE4EFFB);
                }
            }
        }

        // hills
        int hstep = q == 0 ? 3 : 8;
        for (int x = 0; x < w; x += hstep) {
            int top = h - (int) hill(x, 1.3);
            g.fill(x, top, x + hstep, h, 0xF0D4E6F8);
        }

        // pines: 3 stacked tiers + trunk each
        if (scenic && q < 2) {
            for (int i = 0; i * 26 < w + 26; i += (q == 0 ? 1 : 3)) {
                int x = i * 26 + (i * 37 % 13);
                int hgt = 22 + (i * 53 % 4) * 7;
                int base = h - (int) hill(x, 1.3) + 5;
                int dark = (i % 2 == 0) ? 0xFF0F2B26 : 0xFF133530;
                g.fill(x - 1, base, x + 1, base + 3, 0xFF2A1F1A);
                g.fill(x - hgt / 3, base - hgt / 3, x + hgt / 3 + 1, base, dark);
                g.fill(x - hgt / 4, base - 2 * hgt / 3, x + hgt / 4 + 1, base - hgt / 3, dark);
                g.fill(x - hgt / 8, base - hgt, x + hgt / 8 + 1, base - 2 * hgt / 3, dark);
                g.fill(x - hgt / 3, base - hgt / 3 - 1, x + hgt / 3 + 1, base - hgt / 3, 0xCCEAF4FF);
            }
        }
        for (int x = 0; x < w; x += hstep) {
            double hh = 6 + 5 * Math.sin(x * 0.02 + 4) + 3 * Math.sin(x * 0.05);
            int top = h - (int) hh;
            g.fill(x, top, x + hstep, h, 0xF2EEF6FF);
        }

        // snow
        int target = ClickGuiModule.snowAmount() * (q == 0 ? 8 : q == 1 ? 5 : 3) / 10;
        while (FLAKES.size() < target) FLAKES.add(newFlake(w, h, true));
        while (FLAKES.size() > target) FLAKES.remove(FLAKES.size() - 1);
        float speed = ClickGuiModule.snowSpeed();
        for (int i = 0; i < FLAKES.size(); i++) {
            Flake f = FLAKES.get(i);
            f.y += f.vy * speed * dt;
            f.x += (float) Math.sin(t * 0.9 + f.phase) * f.drift * dt + 3f * dt;
            if (f.y > h + 4 || f.x > w + 8) { FLAKES.set(i, newFlake(w, h, false)); continue; }
            if (f.x < -8) f.x = w + 4;
            int c = ((90 + f.layer * 55) << 24) | 0xFFFFFF;
            int x = (int) f.x, y = (int) f.y, sz = (int) f.size;
            g.fill(x, y, x + sz, y + sz, c);
        }

        // frost edges
        if (q < 2) {
            for (int i = 0; i < 24; i += 6) {
                int a = (int) (70 * (1 - i / 24f) * (1 - i / 24f));
                int col = (a << 24) | 0xCFE8FF;
                g.fill(i, 0, i + 6, h, col);
                g.fill(w - i - 6, 0, w - i, h, col);
            }
        }
    }
}
