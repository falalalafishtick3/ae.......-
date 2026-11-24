package dev.aether.module.impl;

import dev.aether.hud.HudUtil;
import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Watermark extends ClientModule {
    private final ModeSetting corner = add(new ModeSetting("Position", "Top Left", "Top Right", "Bottom Left", "Bottom Right"));
    private final NumberSetting offX = add(new NumberSetting("Offset X", 6, 0, 300, 1));
    private final NumberSetting offY = add(new NumberSetting("Offset Y", 6, 0, 300, 1));
    private final BoolSetting fps = add(new BoolSetting("Show FPS", true));
    private final BoolSetting time = add(new BoolSetting("Show Time", true));

    public Watermark() { super("Watermark", "AETHER CLIENT tag", Category.HUD); }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        var tr = mc.textRenderer;
        StringBuilder info = new StringBuilder();
        if (fps.get()) info.append(mc.getCurrentFps()).append(" fps");
        if (time.get()) { if (info.length() > 0) info.append("  |  "); info.append(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))); }
        String name = "AETHER";
        int nw = tr.getWidth(name), iw = info.length() == 0 ? 0 : tr.getWidth(info.toString()) + 10;
        int w = nw + iw + 14, h = 16;
        int[] p = HudUtil.pos(corner.get(), offX.i(), offY.i(), w, h, g.getScaledWindowWidth(), g.getScaledWindowHeight());
        dev.aether.hud.HudUtil.card(g, p[0], p[1], w, h, ClickGuiModule.accent());
        double t = Util.getMeasuringTimeMs() / 1000.0;
        for (int i = 0; i < w; i += 2) {
            int col = 0xFF000000 | MathHelper.hsvToRgb((float) (ClickGuiModule.baseHue() + 0.05 * (0.5 + 0.5 * Math.sin(t + i * 0.03))), ClickGuiModule.sat(), 1f);
            g.fill(p[0] + i, p[1], p[0] + Math.min(w, i + 2), p[1] + 1, col);
        }
        int x = p[0] + 7;
        for (int i = 0; i < name.length(); i++) {
            String ch = String.valueOf(name.charAt(i));
            int col = 0xFF000000 | MathHelper.hsvToRgb((float) (ClickGuiModule.baseHue() + 0.04 * Math.sin(t * 1.4 + i * 0.7)), ClickGuiModule.sat(), 1f);
            g.drawText(tr, ch, x, p[1] + 5, col, true);
            x += tr.getWidth(ch);
        }
        if (iw > 0) g.drawText(tr, info.toString(), p[0] + nw + 17, p[1] + 5, 0xFFBBBBBB, false);
    }
}
