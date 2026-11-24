package dev.aether.module.impl;

import dev.aether.hud.HudUtil;
import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

public class ArmorHud extends ClientModule {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private final ModeSetting corner = add(new ModeSetting("Position", "Bottom Right", "Top Left", "Top Right", "Bottom Left"));
    private final NumberSetting offX = add(new NumberSetting("Offset X", 8, 0, 300, 1));
    private final NumberSetting offY = add(new NumberSetting("Offset Y", 90, 0, 300, 1));
    private final BoolSetting percent = add(new BoolSetting("Durability %", true));

    public ArmorHud() { super("ArmorHud", "Shows your armor and durability", Category.HUD); }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        var tr = mc.textRenderer;
        int w = percent.get() ? 52 : 20, h = 4 * 18;
        int[] p = HudUtil.pos(corner.get(), offX.i(), offY.i(), w, h, g.getScaledWindowWidth(), g.getScaledWindowHeight());
        int y = p[1];
        for (EquipmentSlot s : SLOTS) {
            ItemStack st = mc.player.getEquippedStack(s);
            if (!st.isEmpty()) {
                g.drawItem(st, p[0], y);
                if (percent.get() && st.isDamageable()) {
                    float f = 1f - st.getDamage() / (float) st.getMaxDamage();
                    int col = 0xFF000000 | MathHelper.hsvToRgb(0.33f * MathHelper.clamp(f, 0f, 1f), 0.8f, 1f);
                    g.drawText(tr, Math.round(f * 100) + "%", p[0] + 20, y + 5, col, true);
                }
            }
            y += 18;
        }
    }
}
