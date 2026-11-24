package dev.aether.module.impl;

import dev.aether.hud.HudUtil;
import dev.aether.hud.SpotifyTracker;
import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Util;

/** Now-playing card for the Spotify desktop app. */
public class SpotifyHud extends ClientModule {
    private final ModeSetting mode      = add(new ModeSetting("Show", "While Playing", "On Song Change", "Always"));
    private final NumberSetting showFor = add(new NumberSetting("Show Duration (s)", 6, 1, 30, 1));
    private final ModeSetting corner    = add(new ModeSetting("Position", HudUtil.CORNERS));
    private final NumberSetting offX    = add(new NumberSetting("Offset X", 8, 0, 400, 1));
    private final NumberSetting offY    = add(new NumberSetting("Offset Y", 8, 0, 400, 1));
    private final NumberSetting scale   = add(new NumberSetting("Scale", 1.0, 0.6, 2.0, 0.05));
    private final NumberSetting bgOp    = add(new NumberSetting("Background Opacity", 75, 0, 100, 1));
    private final BoolSetting progress  = add(new BoolSetting("Show Progress", true));
    private final BoolSetting equalizer = add(new BoolSetting("Equalizer", true));
    private final BoolSetting showArtist= add(new BoolSetting("Show Artist", true));
    private final NumberSetting poll    = add(new NumberSetting("Refresh (s)", 5, 1, 15, 1));
    private final ColorSetting accent   = add(new ColorSetting("Accent", 0.39f, 0.85f, 0.73f));

    private float alpha;
    private long last = System.nanoTime();

    public SpotifyHud() {
        super("SpotifyHud", "Shows the song playing in Spotify", Category.HUD);
        showFor.visibleWhen(() -> mode.is("On Song Change"));
    }

    @Override public void onEnable() { SpotifyTracker.ensureRunning(); }
    @Override public void onDisable() { SpotifyTracker.shutdown(); alpha = 0; }

    private static String time(long ms) { long s = Math.max(0, ms / 1000); return (s / 60) + ":" + String.format("%02d", s % 60); }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        SpotifyTracker.ensureRunning();
        SpotifyTracker.intervalMs = poll.i() * 1000;

        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f); last = now;

        boolean hasTrack = !SpotifyTracker.title.isEmpty();
        boolean show = switch (mode.get()) {
            case "Always" -> true;
            case "On Song Change" -> hasTrack && SpotifyTracker.running
                    && System.currentTimeMillis() - SpotifyTracker.changedAt < showFor.i() * 1000L;
            default -> SpotifyTracker.playing;
        };
        alpha += ((show ? 1f : 0f) - alpha) * Math.min(1f, dt * 8f);
        if (alpha < 0.04f) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        float s = scale.f();
        int w = 156, h = progress.get() && SpotifyTracker.hasProgress ? 46 : 38;
        int sw = g.getScaledWindowWidth(), sh = g.getScaledWindowHeight();
        int[] p = HudUtil.pos(corner.get(), offX.i(), offY.i(), Math.round(w * s), Math.round(h * s), sw, sh);

        int acc = 0xFF000000 | accent.rgb();
        var m = g.getMatrices();
        m.pushMatrix();
        m.translate(p[0], p[1]);
        m.scale(s, s);

        // card
        int bg = HudUtil.a(0xFF0C0D10, bgOp.f() / 100f * alpha);
        dev.aether.gui.Ui.rrect(g, -1, -1, w + 2, h + 2, 6, HudUtil.a(0x22FFFFFF, alpha));
        dev.aether.gui.Ui.rrect(g, 0, 0, w, h, 5, bg);
        dev.aether.gui.Ui.rrect(g, 1, 4, 2, h - 8, 1, HudUtil.a(acc, alpha));

        // equalizer
        double t = Util.getMeasuringTimeMs() / 1000.0;
        if (equalizer.get()) {
            for (int i = 0; i < 4; i++) {
                int bh = SpotifyTracker.playing ? 4 + (int) (10 * (0.5 + 0.5 * Math.sin(t * (5 + i * 1.7) + i * 1.9))) : 3;
                g.fill(9 + i * 4, 26 - bh, 11 + i * 4, 26, HudUtil.a(acc, alpha));
            }
        }

        // text
        int tx = equalizer.get() ? 30 : 10;
        int maxW = w - tx - 8;
        String title = hasTrack ? SpotifyTracker.title : (SpotifyTracker.running ? "Nothing playing" : "Spotify not detected");
        g.drawText(tr, tr.trimToWidth(title, maxW), tx, 8, HudUtil.a(0xFFFFFFFF, alpha), false);
        if (showArtist.get() && hasTrack)
            g.drawText(tr, tr.trimToWidth(SpotifyTracker.artist, maxW), tx, 20, HudUtil.a(0xFFB3B3B3, alpha), false);
        if (!SpotifyTracker.playing && hasTrack)
            g.drawText(tr, "paused", w - 6 - tr.getWidth("paused"), 8, HudUtil.a(0xFF777777, alpha), false);

        // progress
        if (progress.get() && SpotifyTracker.hasProgress && SpotifyTracker.lenMs > 0) {
            int bx = 8, bw = w - 16, by = h - 9;
            float f = Math.min(1f, SpotifyTracker.position() / (float) SpotifyTracker.lenMs);
            g.fill(bx, by, bx + bw, by + 2, HudUtil.a(0x44FFFFFF, alpha));
            g.fill(bx, by, bx + (int) (bw * f), by + 2, HudUtil.a(acc, alpha));
            g.fill(bx + (int) (bw * f) - 1, by - 1, bx + (int) (bw * f) + 1, by + 3, HudUtil.a(0xFFFFFFFF, alpha));
        }
        m.popMatrix();
    }
}
