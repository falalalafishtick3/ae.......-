package dev.aether.module.impl;

import dev.aether.hud.HudUtil;
import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;
import java.util.ArrayList;
import java.util.List;

public class PotionHud extends ClientModule {
    private final ModeSetting corner = add(new ModeSetting("Position", "Top Left", "Top Right", "Bottom Left", "Bottom Right"));
    private final NumberSetting offX = add(new NumberSetting("Offset X", 6, 0, 300, 1));
    private final NumberSetting offY = add(new NumberSetting("Offset Y", 70, 0, 300, 1));

    public PotionHud() { super("PotionHud", "Lists your active effects", Category.HUD); }

    private static String roman(int n) {
        return switch (n) { case 1 -> ""; case 2 -> " II"; case 3 -> " III"; case 4 -> " IV"; case 5 -> " V"; default -> " " + n; };
    }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        var tr = mc.textRenderer;
        List<String> names = new ArrayList<>(), times = new ArrayList<>(); List<Integer> cols = new ArrayList<>();
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            names.add(e.getEffectType().value().getName().getString() + roman(e.getAmplifier() + 1));
            int s = e.getDuration() / 20;
            times.add(e.isInfinite() ? "inf" : (s / 60) + ":" + String.format("%02d", s % 60));
            cols.add(!e.isInfinite() && s < 10 ? 0xFFF87171 : 0xFFFFFFFF);
        }
        if (names.isEmpty()) return;
        int w = 0;
        for (int i = 0; i < names.size(); i++) w = Math.max(w, tr.getWidth(names.get(i)) + tr.getWidth(times.get(i)) + 18);
        int h = names.size() * 11 + 4;
        int[] p = HudUtil.pos(corner.get(), offX.i(), offY.i(), w, h, g.getScaledWindowWidth(), g.getScaledWindowHeight());
        HudUtil.card(g, p[0], p[1], w, h, ClickGuiModule.accent());
        int y = p[1] + 3;
        for (int i = 0; i < names.size(); i++) {
            g.drawText(tr, names.get(i), p[0] + 6, y, 0xFFFFFFFF, true);
            g.drawText(tr, times.get(i), p[0] + w - 6 - tr.getWidth(times.get(i)), y, cols.get(i), true);
            y += 11;
        }
    }
}
