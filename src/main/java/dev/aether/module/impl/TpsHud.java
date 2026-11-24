package dev.aether.module.impl;

import dev.aether.hud.HudUtil;
import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;

/** Estimates server TPS from the spacing of world-time packets (hook: ClientPlayNetworkHandlerMixin). */
public class TpsHud extends ClientModule {
    public static TpsHud INSTANCE;
    private final ModeSetting corner = add(new ModeSetting("Position", "Top Left", "Top Right", "Bottom Left", "Bottom Right"));
    private final NumberSetting offX = add(new NumberSetting("Offset X", 6, 0, 400, 1));
    private final NumberSetting offY = add(new NumberSetting("Offset Y", 100, 0, 400, 1));

    private long last;
    private final double[] hist = new double[5];
    private int n;
    private volatile double tps = 20;

    public TpsHud() { super("TpsHud", "Server TPS estimate", Category.HUD); INSTANCE = this; }

    public void onTimePacket() {
        long now = System.nanoTime();
        if (last != 0) {
            double dt = (now - last) / 1e9;
            if (dt > 0.05) {
                hist[n++ % hist.length] = MathHelper.clamp(20.0 / dt, 0, 20);
                int c = Math.min(n, hist.length); double s = 0;
                for (int i = 0; i < c; i++) s += hist[i];
                tps = s / c;
            }
        }
        last = now;
    }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        var tr = MinecraftClient.getInstance().textRenderer;
        double since = last == 0 ? 0 : (System.nanoTime() - last) / 1e9;
        String t = last == 0 ? "TPS: ..." : String.format("TPS: %.1f", tps);
        String s2 = since > 2.5 ? String.format("no update %.1fs", since) : null;
        int w = Math.max(tr.getWidth(t), s2 == null ? 0 : tr.getWidth(s2)) + 10, h = s2 == null ? 16 : 26;
        int[] p = HudUtil.pos(corner.get(), offX.i(), offY.i(), w, h, g.getScaledWindowWidth(), g.getScaledWindowHeight());
        HudUtil.card(g, p[0], p[1], w, h, ClickGuiModule.accent());
        int col = 0xFF000000 | MathHelper.hsvToRgb(0.33f * (float) MathHelper.clamp((tps - 8) / 12.0, 0, 1), 0.75f, 1f);
        g.drawText(tr, t, p[0] + 6, p[1] + 4, col, true);
        if (s2 != null) g.drawText(tr, s2, p[0] + 6, p[1] + 15, 0xFFF87171, false);
    }
}
