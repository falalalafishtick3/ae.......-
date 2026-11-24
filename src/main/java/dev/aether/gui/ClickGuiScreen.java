package dev.aether.gui;

import dev.aether.config.ConfigManager;
import dev.aether.module.*;
import dev.aether.module.impl.ClickGuiModule;
import dev.aether.setting.*;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.Click;               // VERSION-SENSITIVE (1.21.9+ input events)
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class ClickGuiScreen extends Screen {
    private static final int W = 120, HH = 22, MH = 18, SH = 16, BAR = 9, PAD = 4;
    private static final int PANEL_BG = 0xE60D1015, INNER_BG = 0xB0080A0E, TEXT = 0xFFD5DAE2, DIM = 0xFF7D8694;

    private static class Panel {
        final Category cat; int x, y; boolean open = true; float openAnim = 1f;
        Panel(Category c, int x, int y) { cat = c; this.x = x; this.y = y; }
    }
    private interface Action { void run(double mx, double my, int button); }
    private record Hit(int x, int y, int w, int h, Action click, Action drag) {
        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private static final Map<ClientModule, float[]> ANIM = new IdentityHashMap<>();   // [hover, enabled, expand]
    private static List<Panel> panels;
    private static com.google.gson.JsonArray pendingLayout;

    /** Panel positions + open state, stored in aether.json so your layout survives restarts. */
    public static com.google.gson.JsonArray exportLayout() {
        if (panels == null) return pendingLayout != null ? pendingLayout : new com.google.gson.JsonArray();
        com.google.gson.JsonArray a = new com.google.gson.JsonArray();
        for (Panel p : panels) {
            com.google.gson.JsonObject o = new com.google.gson.JsonObject();
            o.addProperty("x", p.x); o.addProperty("y", p.y); o.addProperty("open", p.open);
            a.add(o);
        }
        return a;
    }
    public static void importLayout(com.google.gson.JsonArray a) {
        if (panels == null) { pendingLayout = a; return; }
        applyLayout(a);
    }
    private static void applyLayout(com.google.gson.JsonArray a) {
        for (int i = 0; i < Math.min(a.size(), panels.size()); i++) {
            var o = a.get(i).getAsJsonObject();
            panels.get(i).x = o.get("x").getAsInt(); panels.get(i).y = o.get("y").getAsInt();
            panels.get(i).open = o.get("open").getAsBoolean(); panels.get(i).openAnim = panels.get(i).open ? 1f : 0f;
        }
    }
    private final List<Hit> hits = new ArrayList<>();
    private Hit activeDrag; private int dragBtn;
    private Panel draggingPanel; private double dragX, dragY;
    private double scroll;
    private ClientModule binding;
    private String query = "";

    private float dt, openT;
    private long lastNs = System.nanoTime();
    private final long openNs = System.nanoTime();
    private ClientModule hoverMod; private long hoverSince; private ClientModule hoverPrev;

    public ClickGuiScreen() {
        super(Text.literal("Aether"));
        if (panels == null) {
            panels = new ArrayList<>();
            int i = 0;
            panels.add(new Panel(null, 12 + i++ * (W + 10), 70));   // Favorites
            for (Category c : Category.values()) panels.add(new Panel(c, 12 + i++ * (W + 10), 70));
            if (pendingLayout != null) { try { applyLayout(pendingLayout); } catch (Exception ignored) {} pendingLayout = null; }
        }
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void close() { ConfigManager.save(); super.close(); }

    private float approach(float v, float target, float speed) { return v + (target - v) * (1f - (float) Math.exp(-dt * speed)); }
    private static float[] an(ClientModule m) { return ANIM.computeIfAbsent(m, k -> new float[]{0f, k.enabled ? 1f : 0f, k.expanded ? 1f : 0f}); }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        long now = System.nanoTime();
        dt = Math.min(0.05f, (now - lastNs) / 1e9f); lastNs = now;
        openT = (now - openNs) / 1e9f / 0.7f;
        hits.clear();
        hoverPrev = hoverMod; hoverMod = null;

        g.fill(0, 0, width, height, Ui.alpha(ClickGuiModule.dimARGB() | 0xFF000000, Ui.ease(Math.min(1f, openT * 2f)) * ((ClickGuiModule.dimARGB() >>> 24) / 255f)));
        if (ClickGuiModule.winter()) dev.aether.render.WinterBackground.draw(g, width, height, false);
        drawCursorGlow(g, mx, my);
        drawBanner(g);
        for (int i = 0; i < panels.size(); i++) drawPanel(g, panels.get(i), i, mx, my);
        drawSearch(g, mx, my);
        button(g, "Bases", 10, height - 30, 52, mx, my, () -> MinecraftClient.getInstance().setScreen(new HubScreen(0)));
        button(g, "Waypoints", 66, height - 30, 68, mx, my, () -> MinecraftClient.getInstance().setScreen(new HubScreen(1)));
        g.drawText(textRenderer, "Right-click a module: settings + Keybind   |   Shift+click: favorite   |   Type to search", 10, height - 12, 0x66FFFFFF, false);
        drawTooltip(g, mx, my);
    }

    private static boolean shiftDown() {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT);
    }

    private void drawCursorGlow(DrawContext g, int mx, int my) {
        int rgb = ClickGuiModule.accent() & 0xFFFFFF;
        Ui.rrect(g, mx - 36, my - 36, 72, 72, 11, Ui.withAlpha(rgb, 6));
        Ui.rrect(g, mx - 22, my - 22, 44, 44, 11, Ui.withAlpha(rgb, 9));
        Ui.rrect(g, mx - 10, my - 10, 20, 20, 9, Ui.withAlpha(rgb, 12));
    }

    private void button(DrawContext g, String label, int x, int y, int w, int mx, int my, Runnable r) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + 18;
        int ac = ClickGuiModule.accent();
        Ui.shadow(g, x, y, w, 18, 9, 40);
        Ui.rrect(g, x, y, w, 18, 9, hov ? Ui.lerp(0xF0141A22, ac, 0.25f) : 0xF0101419);
        Ui.rrect(g, x, y, w, 18, 9, Ui.withAlpha(ac, hov ? 0x40 : 0x18));
        g.drawText(textRenderer, label, x + (w - textRenderer.getWidth(label)) / 2, y + 5, 0xFFFFFFFF, false);
        hits.add(new Hit(x, y, w, 18, (a, b, btn) -> { if (btn == 0) r.run(); }, null));
    }

    // ---------------- title banner ----------------
    private static int rgbArgb(float h, float sat, float br, int a) {
        return (a << 24) | (MathHelper.hsvToRgb(h - (float) Math.floor(h), sat, br) & 0xFFFFFF);
    }
    private static int mix(int rgb, int to, float k) {
        int r = (int) (((rgb >> 16 & 255) * (1 - k)) + ((to >> 16 & 255) * k));
        int gr = (int) (((rgb >> 8 & 255) * (1 - k)) + ((to >> 8 & 255) * k));
        int b = (int) (((rgb & 255) * (1 - k)) + ((to & 255) * k));
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }
    private void scaledText(DrawContext g, String str, float x, float y, float scale, int argb, boolean shadow) {
        var m = g.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);
        g.drawText(textRenderer, str, 0, 0, argb, shadow);
        m.popMatrix();
    }
    /** Horizontal line that fades out toward both ends (alpha ramps), drawn in short segments. */
    private void fadeLine(DrawContext g, int x0, int x1, int y, int h, float hue, boolean fadeLeft) {
        int n = Math.max(1, x1 - x0);
        for (int i = 0; i < n; i += 8) {
            float t = i / (float) n;
            float a = fadeLeft ? t : 1f - t;
            g.fill(x0 + i, y, Math.min(x1, x0 + i + 8), y + h, rgbArgb(hue + t * 0.08f, 0.55f, 1f, (int) (210 * a * a)));
        }
    }

    private void drawBanner(DrawContext g) {
        double t = Util.getMeasuringTimeMs() / 1000.0;
        final String title = "AETHER";
        final float scale = 3.2f;
        final int spacing = 3;
        int[] cw = new int[title.length()];
        int total = 0;
        for (int i = 0; i < title.length(); i++) { cw[i] = textRenderer.getWidth(String.valueOf(title.charAt(i))); total += cw[i] + spacing; }
        total -= spacing;
        float tw = total * scale;
        float x = (width - tw) / 2f, y = 9f;

        // soft backdrop glow behind the title
        for (int i = 0; i < 6; i++) {
            int pad = 6 + i * 5;
            g.fill((int) (x - pad), (int) (y - 2 - i), (int) (x + tw + pad), (int) (y + 26 + i), (6 - i) << 24 | 0x0B2A33);
        }

        // side lines
        int cx = width / 2;
        int lineY = (int) (y + 12);
        float baseHue = ClickGuiModule.baseHue() + (float) Math.sin(t * 0.6) * 0.03f;
        fadeLine(g, Math.max(8, cx - 230), (int) (x - 14), lineY, 1, baseHue, true);
        fadeLine(g, (int) (x + tw + 14), Math.min(width - 8, cx + 230), lineY, 1, baseHue, false);
        g.fill((int) (x - 10), lineY - 2, (int) (x - 8), lineY + 3, rgbArgb(baseHue, 0.5f, 1f, 255));
        g.fill((int) (x + tw + 8), lineY - 2, (int) (x + tw + 10), lineY + 3, rgbArgb(baseHue, 0.5f, 1f, 255));

        // letters: animated teal -> cyan -> violet wave, glow pass, then a white shimmer sweeping across
        float shimmer = (float) ((t * 5.0) % (title.length() + 8)) - 3f;
        float cursor = x;
        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            float hue = ClickGuiModule.baseHue() + 0.01f + 0.13f * (float) (0.5 + 0.5 * Math.sin(t * 1.4 + i * 0.75));
            int rgb = MathHelper.hsvToRgb(hue, ClickGuiModule.sat(), 1f);
            float k = Math.max(0f, 1f - Math.abs(i - shimmer) / 2.2f);
            int col = mix(rgb, 0xFFFFFF, k * 0.85f);
            float bob = (float) Math.sin(t * 2.0 + i * 0.5) * 0.8f;
            // glow
            int glow = (0x38 << 24) | (rgb & 0xFFFFFF);
            for (int[] o : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}})
                scaledText(g, ch, cursor + o[0] * 1.1f, y + bob + o[1] * 1.1f, scale, glow, false);
            scaledText(g, ch, cursor, y + bob, scale, col, true);
            cursor += (cw[i] + spacing) * scale;
        }

        // subtitle
        final String sub = "C L I E N T";
        float ss = 1.25f;
        float sw = textRenderer.getWidth(sub) * ss;
        float sx = (width - sw) / 2f, sy = y + 28f;
        for (int i = 0; i < sub.length(); i++) {
            String ch = String.valueOf(sub.charAt(i));
            float h = ClickGuiModule.baseHue() + 0.04f + 0.1f * (float) Math.sin(t * 1.2 + i * 0.4);
            scaledText(g, ch, sx, sy, ss, rgbArgb(h, 0.35f, 1f, 235), true);
            sx += textRenderer.getWidth(ch) * ss;
        }

        // underline accent + version
        int ux = (int) ((width - 90) / 2f);
        for (int i = 0; i < 90; i += 6) {
            float tt = i / 90f;
            float edge = 1f - Math.abs(tt - 0.5f) * 2f;
            g.fill(ux + i, (int) (sy + 13), ux + i + 6, (int) (sy + 14), rgbArgb(baseHue + tt * 0.1f, 0.6f, 1f, (int) (255 * edge)));
        }
        String ver = "1.21.11";
        g.drawText(textRenderer, ver, width - textRenderer.getWidth(ver) - 8, 8, 0x66FFFFFF, false);
    }

    // ---------------- panels ----------------
    private List<ClientModule> visibleMods(Panel p) {
        List<ClientModule> base = p.cat == null ? ModuleManager.MODULES.stream().filter(mm -> mm.favorite).toList() : ModuleManager.in(p.cat);
        if (query.isEmpty()) return base;
        String q = query.toLowerCase();
        return base.stream().filter(m -> m.name.toLowerCase().contains(q)).toList();
    }

    private static int settingH(Setting<?> s) { return s instanceof ColorSetting ? SH + BAR * 3 + 4 : SH; }
    private static int openH(ClientModule m) {
        int h = SH + 6;
        for (Setting<?> s : m.settings) if (s.isVisible()) h += settingH(s);
        return h;
    }

    private void drawPanel(DrawContext g, Panel p, int idx, int mx, int my) {
        int ac = ClickGuiModule.accent();
        float pt = Math.max(0f, Math.min(1f, (openT - idx * 0.08f) / 0.6f));
        float pe = 1f - (1f - pt) * (1f - pt) * (1f - pt);
        boolean live = pt >= 0.999f;
        int sl = Math.round(-(1f - pe) * 18f);
        int x = p.x, y = p.y + (int) scroll;
        List<ClientModule> mods = visibleMods(p);

        // measure body (also advances expand animations)
        int bodyH = PAD + 2;
        for (ClientModule m : mods) {
            float[] a = an(m);
            a[2] = approach(a[2], m.expanded ? 1f : 0f, 14f);
            bodyH += MH + 1 + Math.round(openH(m) * Ui.ease(a[2]));
        }
        bodyH += PAD;
        p.openAnim = approach(p.openAnim, p.open ? 1f : 0f, 16f);
        float oe = Ui.ease(p.openAnim);
        int shown = Math.round(bodyH * oe);

        var mat = g.getMatrices();
        mat.pushMatrix();
        mat.translate(0f, (float) sl);

        // header
        Ui.shadow(g, x, y, W, HH, 7, 70);
        Ui.rrect(g, x - 1, y - 1, W + 2, HH + 2, 8, 0x28FFFFFF);
        Ui.rrectV(g, x, y, W, HH, 7, Ui.lighten(ac, 0.18f), Ui.darken(ac, 0.30f));
        String label = p.cat == null ? "Favorites" : p.cat.label;
        Ui.circle(g, x + 10, y + HH / 2, 2, 0xCCFFFFFF);
        g.drawText(textRenderer, label, x + (W - textRenderer.getWidth(label)) / 2, y + 7, 0xFFFFFFFF, true);
        String cnt = String.valueOf(mods.size());
        g.drawText(textRenderer, cnt, x + W - 8 - textRenderer.getWidth(cnt), y + 7, 0x99FFFFFF, false);
        if (live) hits.add(new Hit(x, y + sl, W, HH, (a, b, btn) -> {
            if (btn == 0) { draggingPanel = p; dragX = a - p.x; dragY = b - (p.y + scroll); }
            else if (btn == 1) p.open = !p.open;
        }, null));

        if (shown > 2) {
            int by = y + HH + 3;
            Ui.shadow(g, x, by, W, shown, 7, 60);
            Ui.rrect(g, x - 1, by - 1, W + 2, shown + 2, 8, 0x1CFFFFFF);
            Ui.rrect(g, x, by, W, shown, 7, PANEL_BG);
            g.enableScissor(x, by + sl, x + W, by + shown + sl);
            boolean interactive = live && p.openAnim > 0.99f;
            int cy = by + PAD + 1;
            for (ClientModule m : mods) cy = drawModule(g, p, m, x, cy, mx, my, sl, interactive, ac);
            g.disableScissor();
        }
        mat.popMatrix();
    }

    private void drawSwitch(DrawContext g, int x, int y, int w, int h, float t, int ac) {
        Ui.rrect(g, x, y, w, h, h / 2, Ui.lerp(0xFF2A2F3A, ac, t));
        int kx = x + 1 + Math.round(t * (w - h));
        Ui.rrect(g, kx, y + 1, h - 2, h - 2, 2, 0xFFFFFFFF);
    }

    private int drawModule(DrawContext g, Panel p, ClientModule m, int x, int cy, int mx, int my, int sl, boolean live, int ac) {
        int rx = x + PAD, rw = W - 2 * PAD;
        float[] a = an(m);
        // cull rows that are entirely off screen (still advances their animation + layout)
        if (cy + sl > height + 4 || cy + sl + MH + openH(m) < -4) {
            a[1] = approach(a[1], m.enabled ? 1f : 0f, 14f);
            return cy + MH + 1 + Math.round(openH(m) * Ui.ease(a[2]));
        }
        boolean hov = live && mx >= rx && mx < rx + rw && my - sl >= cy && my - sl < cy + MH;
        if (hov) { hoverMod = m; if (hoverPrev != m) hoverSince = System.currentTimeMillis(); }
        a[0] = approach(a[0], hov ? 1f : 0f, 18f);
        a[1] = approach(a[1], m.enabled ? 1f : 0f, 14f);

        if (a[1] > 0.02f) Ui.rrect(g, rx, cy, rw, MH, 5, Ui.withAlpha(ac & 0xFFFFFF, (int) (70 * a[1])));
        if (a[0] > 0.02f) Ui.rrect(g, rx, cy, rw, MH, 5, Ui.withAlpha(0xFFFFFF, (int) (22 * a[0])));
        if (a[1] > 0.02f) Ui.rrect(g, rx, cy + 3, 2, MH - 6, 1, Ui.withAlpha(ac & 0xFFFFFF, (int) (255 * a[1])));

        int swW = 18, swH = 9, swX = rx + rw - 14 - swW, swY = cy + (MH - swH) / 2;
        drawSwitch(g, swX, swY, swW, swH, a[1], ac);
        g.drawText(textRenderer, m.expanded ? "v" : ">", rx + rw - 9, cy + 5, Ui.lerp(0xFF59616E, 0xFFFFFFFF, a[2]), false);

        String kl = binding == m ? "..." : (m.key >= 0 ? keyName(m.key) : null);
        int klw = kl == null ? 0 : textRenderer.getWidth(kl) + 6;
        if (kl != null) g.drawText(textRenderer, kl, swX - 4 - textRenderer.getWidth(kl), cy + 5, binding == m ? 0xFFFFC857 : 0xFF6F7886, false);
        int star = m.favorite ? 8 : 0;
        int avail = swX - klw - (rx + 8) - 6 - star;
        String nm = textRenderer.trimToWidth(m.name, Math.max(10, avail));
        int nameCol = Ui.lerp(0xFF8F98A6, 0xFFFFFFFF, Math.max(a[1], a[0] * 0.6f));
        g.drawText(textRenderer, nm, rx + 8, cy + 5, nameCol, a[1] > 0.5f);
        if (m.favorite) g.drawText(textRenderer, "*", rx + 8 + textRenderer.getWidth(nm) + 2, cy + 5, 0xFFFFC857, false);

        if (live) hits.add(new Hit(rx, cy + sl, rw, MH, (aa, b, btn) -> {
            if (btn == 0) { if (shiftDown()) m.favorite = !m.favorite; else m.toggle(); }
            else if (btn == 1) m.expanded = !m.expanded;
            else if (btn == 2) binding = binding == m ? null : m;
        }, null));
        cy += MH + 1;

        int open = Math.round(openH(m) * Ui.ease(a[2]));
        if (open > 1) {
            Ui.rrect(g, rx, cy - 1, rw, open + 1, 5, INNER_BG);
            g.enableScissor(rx, cy + sl, rx + rw, cy + open + sl);
            boolean full = live && a[2] > 0.99f;
            int sy = cy + 3;
            sy = drawBindRow(g, m, rx + 3, sy, rw - 6, full, ac, sl);
            for (Setting<?> s : m.settings) if (s.isVisible()) sy = drawSetting(g, s, rx + 3, sy, rw - 6, full, ac, sl);
            g.disableScissor();
        }
        return cy + open;
    }

    private void chip(DrawContext g, String text, int right, int y, int color) {
        int cw = textRenderer.getWidth(text) + 10;
        Ui.rrect(g, right - cw, y + 2, cw, SH - 4, 4, Ui.withAlpha(color & 0xFFFFFF, 0x30));
        g.drawText(textRenderer, text, right - cw + 5, y + 4, color, false);
    }

    /** "Keybind" row shown first inside every expanded module. Left-click = set (then press a key), right-click = clear. */
    private int drawBindRow(DrawContext g, ClientModule m, int x, int y, int w, boolean live, int ac, int sl) {
        g.drawText(textRenderer, "Keybind", x + 3, y + 4, TEXT, false);
        boolean listening = binding == m;
        String v = listening ? "press a key..." : (m.key >= 0 ? keyName(m.key) : "None");
        chip(g, v, x + w - 1, y, listening ? 0xFFFFC857 : ac);
        if (live) hits.add(new Hit(x, y + sl, w, SH, (a, b, btn) -> {
            if (btn == 0) binding = m;
            else if (btn == 1) { m.key = -1; binding = null; }
        }, null));
        return y + SH;
    }

    private int drawSetting(DrawContext g, Setting<?> s, int x, int y, int w, boolean live, int ac, int sl) {
        int ty = y + 4;
        if (s instanceof BoolSetting b) {
            g.drawText(textRenderer, b.name, x + 3, ty, TEXT, false);
            drawSwitch(g, x + w - 17, y + 3, 16, 9, b.get() ? 1f : 0f, ac);
            if (live) hits.add(new Hit(x, y + sl, w, SH, (a, c, btn) -> { if (btn == 0) b.set(!b.get()); }, null));
            return y + SH;
        }
        if (s instanceof NumberSetting n) {
            double f = (n.get() - n.min) / (n.max - n.min);
            Ui.rrect(g, x, y + 1, w, SH - 2, 4, 0x26FFFFFF);
            int fw = Math.max(5, (int) (w * f));
            Ui.rrectV(g, x, y + 1, fw, SH - 2, 4, Ui.withAlpha(ac & 0xFFFFFF, 0xB0), Ui.withAlpha(Ui.darken(ac, 0.25f) & 0xFFFFFF, 0x90));
            g.drawText(textRenderer, n.name, x + 5, ty, 0xFFFFFFFF, false);
            String v = n.step >= 1 ? String.valueOf(n.i()) : String.format("%.2f", n.get());
            g.drawText(textRenderer, v, x + w - 5 - textRenderer.getWidth(v), ty, 0xFFFFFFFF, false);
            Action a = (mx, my, btn) -> n.set(n.min + MathHelper.clamp((mx - x) / w, 0, 1) * (n.max - n.min));
            if (live) hits.add(new Hit(x, y + sl, w, SH, (mx, my, btn) -> { if (btn == 0) a.run(mx, my, btn); }, a));
            return y + SH;
        }
        if (s instanceof ModeSetting md) {
            g.drawText(textRenderer, md.name, x + 3, ty, TEXT, false);
            chip(g, md.get(), x + w - 1, y, ac);
            if (live) hits.add(new Hit(x, y + sl, w, SH, (a, c, btn) -> md.cycle(btn == 1 ? -1 : 1), null));
            return y + SH;
        }
        if (s instanceof ColorSetting c) {
            g.drawText(textRenderer, c.name, x + 3, ty, TEXT, false);
            Ui.circle(g, x + w - 8, y + SH / 2, 5, 0x55FFFFFF);
            Ui.circle(g, x + w - 8, y + SH / 2, 4, 0xFF000000 | c.rgb());
            int by = y + SH;
            for (int i = 0; i < 3; i++) drawBar(g, c, i, x + 2, by + i * BAR + 1, w - 4, live, sl);
            return by + BAR * 3 + 4;
        }
        return y;
    }

    private void drawBar(DrawContext g, ColorSetting c, int type, int bx, int by, int bw, boolean live, int sl) {
        final int seg = 28;
        Ui.rrect(g, bx - 1, by - 1, bw + 2, BAR - 1, 3, 0x40000000);
        for (int k = 0; k < seg; k++) {
            float t = k / (float) seg;
            int col = switch (type) {
                case 0 -> MathHelper.hsvToRgb(t, c.s, c.b);
                case 1 -> MathHelper.hsvToRgb(c.h, t, c.b);
                default -> MathHelper.hsvToRgb(c.h, c.s, t);
            };
            g.fill(bx + k * bw / seg, by, bx + (k + 1) * bw / seg, by + BAR - 3, 0xFF000000 | col);
        }
        float val = type == 0 ? c.h : type == 1 ? c.s : c.b;
        int mxp = bx + (int) (val * bw);
        Ui.rrect(g, mxp - 2, by - 1, 4, BAR - 1, 2, 0xFFFFFFFF);

        Action a = (mx, my, btn) -> {
            float v = MathHelper.clamp((float) (mx - bx) / bw, 0f, 1f);
            if (type == 0) c.h = Math.min(v, 0.999f); else if (type == 1) c.s = v; else c.b = v;
        };
        if (live) hits.add(new Hit(bx, by + sl, bw, BAR, (mx, my, btn) -> { if (btn == 0) a.run(mx, my, btn); }, a));
    }

    private void drawTooltip(DrawContext g, int mx, int my) {
        if (hoverMod == null || hoverMod != hoverPrev || System.currentTimeMillis() - hoverSince < 350 || draggingPanel != null || activeDrag != null) return;
        String d = hoverMod.description;
        if (d == null || d.isEmpty()) return;
        d = textRenderer.trimToWidth(d, 260);
        int w = textRenderer.getWidth(d) + 14, h = 18;
        int x = Math.min(mx + 12, width - w - 6), y = Math.min(my + 14, height - h - 6);
        int ac = ClickGuiModule.accent();
        Ui.shadow(g, x, y, w, h, 6, 70);
        Ui.rrect(g, x - 1, y - 1, w + 2, h + 2, 7, Ui.withAlpha(ac & 0xFFFFFF, 0x55));
        Ui.rrect(g, x, y, w, h, 6, 0xF2090B10);
        g.drawText(textRenderer, d, x + 7, y + 5, 0xFFFFFFFF, false);
    }

    private void drawSearch(DrawContext g, int mx, int my) {
        int w = 200, h = 20, x = (width - w) / 2, y = height - 36;
        int ac = ClickGuiModule.accent();
        boolean active = !query.isEmpty();
        Ui.shadow(g, x, y, w, h, 10, 60);
        Ui.rrect(g, x - 1, y - 1, w + 2, h + 2, 11, Ui.withAlpha(ac & 0xFFFFFF, active ? 0x90 : 0x30));
        Ui.rrect(g, x, y, w, h, 10, 0xF00C0F14);
        // magnifier
        Ui.circle(g, x + 13, y + 9, 4, 0xFF8F98A6);
        Ui.circle(g, x + 13, y + 9, 2, 0xF00C0F14);
        g.fill(x + 16, y + 12, x + 18, y + 14, 0xFF8F98A6);
        boolean caret = (Util.getMeasuringTimeMs() / 500) % 2 == 0;
        String text = query.isEmpty() ? "Search modules..." : query + (caret ? "_" : "");
        g.drawText(textRenderer, text, x + 26, y + 6, active ? 0xFFFFFFFF : 0xFF6F7886, false);
    }

    // ---- VERSION-SENSITIVE: 1.21.9+ passes a Click record instead of (x, y, button) ----
    @Override public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y(); int btn = click.button();
        if (binding != null && btn != 2) binding = null;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.in(mx, my)) {
                h.click().run(mx, my, btn);
                if (h.drag() != null) { activeDrag = h; dragBtn = btn; }
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }
    @Override public boolean mouseDragged(Click click, double dx, double dy) {
        if (draggingPanel != null) {
            draggingPanel.x = (int) (click.x() - dragX);
            draggingPanel.y = (int) (click.y() - dragY - scroll);
            return true;
        }
        if (activeDrag != null && dragBtn == 0) { activeDrag.drag().run(click.x(), click.y(), dragBtn); return true; }
        return super.mouseDragged(click, dx, dy);
    }
    @Override public boolean mouseReleased(Click click) {
        draggingPanel = null; activeDrag = null;
        return super.mouseReleased(click);
    }
    @Override public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = Math.min(0, scroll + v * 20);
        return true;
    }

    @Override public boolean keyPressed(KeyInput input) {
        if (binding != null) {
            int k = input.getKeycode();
            if (k == GLFW.GLFW_KEY_ESCAPE) binding = null;
            else if (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) { binding.key = -1; binding = null; }
            else { binding.key = k; binding = null; }
            return true;
        }
        int key = input.getKeycode();
        if (key == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) { query = query.substring(0, query.length() - 1); return true; }
        if (key == GLFW.GLFW_KEY_ESCAPE && !query.isEmpty()) { query = ""; return true; }
        return super.keyPressed(input);
    }

    private static String keyName(int k) {
        if (k >= GLFW.GLFW_KEY_F1 && k <= GLFW.GLFW_KEY_F12) return "F" + (k - GLFW.GLFW_KEY_F1 + 1);
        String n = GLFW.glfwGetKeyName(k, 0);
        if (n != null) return n.toUpperCase();
        return switch (k) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT"; case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL"; case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT"; case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_UP -> "UP"; case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT"; case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE"; case GLFW.GLFW_KEY_TAB -> "TAB";
            default -> "#" + k;
        };
    }

    @Override public boolean charTyped(CharInput input) {
        if (binding != null) return true;
        String c = input.asString();
        if (c != null && !c.isEmpty() && input.isValidChar() && query.length() < 24) { query += c; scroll = 0; return true; }
        return super.charTyped(input);
    }

}
