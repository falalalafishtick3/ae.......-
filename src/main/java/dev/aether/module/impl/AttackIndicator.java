package dev.aether.module.impl;

import dev.aether.gui.Ui;
import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** Cooldown bar under your crosshair: fills as your weapon recharges, turns green when ready. */
public class AttackIndicator extends ClientModule {
    private final NumberSetting width = add(new NumberSetting("Width", 36, 16, 100, 2));
    private final NumberSetting offY = add(new NumberSetting("Offset Y", 14, 4, 80, 1));
    private final BoolSetting always = add(new BoolSetting("Always Show", false));

    public AttackIndicator() { super("AttackIndicator", "Weapon cooldown bar at your crosshair", Category.HUD); }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        float c = MinecraftClient.getInstance().player.getAttackCooldownProgress(0f);
        if (c >= 1f && !always.get()) return;
        int w = width.i(), x = g.getScaledWindowWidth() / 2 - w / 2, y = g.getScaledWindowHeight() / 2 + offY.i();
        int col = Ui.lerp(0xFFFF5A5A, 0xFF5AFF8A, c);
        Ui.rrect(g, x - 1, y - 1, w + 2, 5, 2, 0x88000000);
        Ui.rrect(g, x, y, Math.max(2, (int) (w * c)), 3, 1, col);
    }
}
