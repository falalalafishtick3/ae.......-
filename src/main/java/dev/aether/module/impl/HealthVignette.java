package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Util;

/** Red screen-edge glow when your health is low. */
public class HealthVignette extends ClientModule {
    private final NumberSetting below = add(new NumberSetting("Below HP", 7, 2, 19, 1));
    private final NumberSetting strength = add(new NumberSetting("Strength", 60, 10, 100, 5));

    public HealthVignette() { super("HealthVignette", "Red edges when low on health", Category.HUD); }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        float hp = MinecraftClient.getInstance().player.getHealth();
        if (hp > below.i()) return;
        float need = 1f - hp / below.f();
        double pulse = 0.7 + 0.3 * Math.sin(Util.getMeasuringTimeMs() / 220.0);
        int a = (int) (strength.f() * 2.2f * need * pulse);
        int w = g.getScaledWindowWidth(), h = g.getScaledWindowHeight(), edge = 36;
        int c1 = (Math.min(255, a) << 24) | 0xE02020;
        g.fillGradient(0, 0, w, edge, c1, 0x00E02020);
        g.fillGradient(0, h - edge, w, h, 0x00E02020, c1);
        for (int i = 0; i < edge; i += 6) {
            int ai = (int) (Math.min(255, a) * (1f - i / (float) edge));
            int c = (ai << 24) | 0xE02020;
            g.fill(i, edge, i + 6, h - edge, c);
            g.fill(w - i - 6, edge, w - i, h - edge, c);
        }
    }
}
