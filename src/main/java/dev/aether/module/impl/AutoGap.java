package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.InvUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;

/** Eats a golden apple when your health gets low. */
public class AutoGap extends ClientModule {
    private final NumberSetting hp = add(new NumberSetting("Eat Below HP", 10, 2, 19, 1));
    private final BoolSetting enchanted = add(new BoolSetting("Allow Enchanted", true));
    private final NumberSetting cooldown = add(new NumberSetting("Cooldown (s)", 3, 1, 20, 1));
    private boolean eating; private int prev = -1, ticks, cd;

    public AutoGap() { super("AutoGap", "Eats golden apples when low on health", Category.COMBAT); }

    private void stop(MinecraftClient mc) {
        mc.options.useKey.setPressed(false);
        if (prev >= 0) InvUtil.select(mc, prev);
        eating = false; prev = -1; ticks = 0; cd = cooldown.i() * 20;
    }

    @Override public void onDisable() { MinecraftClient mc = MinecraftClient.getInstance(); if (eating && mc.player != null) stop(mc); }

    @Override public void onTick(MinecraftClient mc) {
        var p = mc.player;
        if (cd > 0) cd--;
        if (eating) { ticks++; if (ticks > 8 && !p.isUsingItem()) stop(mc); return; }
        if (cd > 0 || mc.currentScreen != null || p.isUsingItem() || p.getHealth() > hp.i()) return;
        int slot = InvUtil.find(mc, s -> s.isOf(Items.GOLDEN_APPLE) || (enchanted.get() && s.isOf(Items.ENCHANTED_GOLDEN_APPLE)));
        if (slot < 0) return;
        prev = InvUtil.selected(mc);
        InvUtil.select(mc, slot);
        mc.options.useKey.setPressed(true);
        eating = true; ticks = 0;
    }
}
