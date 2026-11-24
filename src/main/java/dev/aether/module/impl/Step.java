package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributes;

public class Step extends ClientModule {
    private final NumberSetting height = add(new NumberSetting("Height", 1.0, 0.6, 2.5, 0.1));

    public Step() { super("Step", "Walk up blocks without jumping", Category.MOVEMENT); }

    private void apply(MinecraftClient mc, double h) {
        if (mc.player == null) return;
        var inst = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (inst != null && inst.getBaseValue() != h) inst.setBaseValue(h);
    }

    @Override public void onTick(MinecraftClient mc) { apply(mc, height.get()); }
    @Override public void onDisable() { apply(MinecraftClient.getInstance(), 0.6); }
}
