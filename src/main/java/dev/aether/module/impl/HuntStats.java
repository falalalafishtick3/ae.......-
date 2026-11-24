package dev.aether.module.impl;

import dev.aether.hud.HudUtil;
import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** Session stats for BaseHunter. */
public class HuntStats extends ClientModule {
    public static volatile int rtps, finds;
    public static volatile long startMs;
    public static volatile String status = "idle";

    private final ModeSetting corner = add(new ModeSetting("Position", "Top Left", "Top Right", "Bottom Left", "Bottom Right"));
    private final NumberSetting offX = add(new NumberSetting("Offset X", 6, 0, 400, 1));
    private final NumberSetting offY = add(new NumberSetting("Offset Y", 150, 0, 400, 1));

    public HuntStats() { super("HuntStats", "RTPs, finds and time for BaseHunter", Category.HUD); }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        var tr = MinecraftClient.getInstance().textRenderer;
        long s = startMs == 0 ? 0 : (System.currentTimeMillis() - startMs) / 1000;
        String[] lines = {"Hunt: " + status, String.format("Time: %d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60), "RTPs: " + rtps, "Finds: " + finds};
        int w = 0; for (String l : lines) w = Math.max(w, tr.getWidth(l));
        int[] p = HudUtil.pos(corner.get(), offX.i(), offY.i(), w + 10, lines.length * 10 + 4, g.getScaledWindowWidth(), g.getScaledWindowHeight());
        HudUtil.card(g, p[0], p[1], w + 10, lines.length * 10 + 4, ClickGuiModule.accent());
        int y = p[1] + 3;
        for (String l : lines) { g.drawText(tr, l, p[0] + 6, y, 0xFFFFFFFF, true); y += 10; }
    }
}
