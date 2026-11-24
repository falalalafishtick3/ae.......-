package dev.aether.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * Small drawing toolkit: rounded rectangles, gradients, circles, shadows, colour math.
 * Everything is built from as FEW fill calls as possible (each fill is a GUI draw element, they add up fast).
 */
public final class Ui {
    /** Corner bands for a radius: number of bands (1..4). */
    private static int bands(int r) { return r <= 3 ? r - 1 : r <= 5 ? 2 : r <= 7 ? 3 : 4; }
    private static int inset(int r, int k, int i) {
        double dy = r - (i + 0.5) * r / (double) k;
        return Math.max(1, (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy))));
    }

    public static void rrect(DrawContext g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) return;
        r = Math.min(Math.min(r, 11), Math.min(w, h) / 2);
        if (r <= 0) { g.fill(x, y, x + w, y + h, color); return; }
        if (r == 1) {
            g.fill(x + 1, y, x + w - 1, y + 1, color);
            g.fill(x, y + 1, x + w, y + h - 1, color);
            g.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
            return;
        }
        int k = bands(r);
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < k; i++) {
            int in = inset(r, k, i);
            int y0 = Math.round(i * r / (float) k), y1 = Math.round((i + 1) * r / (float) k);
            if (y1 <= y0) y1 = y0 + 1;
            g.fill(x + in, y + y0, x + w - in, y + y1, color);
            g.fill(x + in, y + h - y1, x + w - in, y + h - y0, color);
        }
    }

    /** Rounded rect with a vertical gradient from c1 (top) to c2 (bottom). */
    public static void rrectV(DrawContext g, int x, int y, int w, int h, int r, int c1, int c2) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(Math.min(r, 11), Math.min(w, h) / 2);
        if (r <= 1) { g.fillGradient(x, y, x + w, y + h, c1, c2); return; }
        int k = bands(r);
        g.fillGradient(x, y + r, x + w, y + h - r, lerp(c1, c2, r / (float) h), lerp(c1, c2, (h - r) / (float) h));
        for (int i = 0; i < k; i++) {
            int in = inset(r, k, i);
            int y0 = Math.round(i * r / (float) k), y1 = Math.round((i + 1) * r / (float) k);
            if (y1 <= y0) y1 = y0 + 1;
            g.fill(x + in, y + y0, x + w - in, y + y1, lerp(c1, c2, (y0 + 0.5f) / h));
            g.fill(x + in, y + h - y1, x + w - in, y + h - y0, lerp(c1, c2, (h - y1 + 0.5f) / h));
        }
    }

    /** Circle drawn as at most ~13 horizontal bands. */
    public static void circle(DrawContext g, int cx, int cy, int r, int color) {
        if (r <= 0) { g.fill(cx, cy, cx + 1, cy + 1, color); return; }
        int s = Math.max(1, r / 6);
        for (int dy = -r; dy <= r; dy += s) {
            double mid = Math.min(r, Math.abs(dy) + s / 2.0);
            int dx = (int) Math.round(Math.sqrt(Math.max(0, r * r - mid * mid)));
            int hgt = Math.min(s, r - dy + 1);
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + hgt, color);
        }
    }

    /** Soft drop shadow under a rounded box (3 cheap layers). */
    public static void shadow(DrawContext g, int x, int y, int w, int h, int r, int strength) {
        for (int i = 1; i <= 3; i++) {
            int a = Math.max(0, strength * (4 - i) / 8);
            rrect(g, x - i * 2, y - i * 2 + 3, w + 4 * i, h + 4 * i, r + i * 2, a << 24);
        }
    }

    // ---------------- colour math ----------------
    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (int) ((a >>> 24) + ((b >>> 24) - (a >>> 24)) * t);
        int rr = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int gg = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bb = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return (aa << 24) | (rr << 16) | (gg << 8) | bb;
    }
    public static int alpha(int argb, float mul) {
        int a = Math.max(0, Math.min(255, Math.round((argb >>> 24) * mul)));
        return (a << 24) | (argb & 0xFFFFFF);
    }
    public static int withAlpha(int rgb, int a) { return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF); }
    public static int lighten(int argb, float k) { return lerp(argb | 0xFF000000, 0xFFFFFFFF, k); }
    public static int darken(int argb, float k) { return lerp(argb | 0xFF000000, 0xFF000000, k); }
    public static float ease(float t) { t = Math.max(0f, Math.min(1f, t)); return t * t * (3f - 2f * t); }
}
