package dev.aether.module.impl;

import dev.aether.mixin.InteractionManagerAccessor;
import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;

public class FastBreak extends ClientModule {
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 0, 0, 5, 1));

    public FastBreak() { super("FastBreak", "Removes the delay between breaking blocks", Category.WORLD); }

    @Override public void onTick(MinecraftClient mc) {
        if (mc.interactionManager != null)
            ((InteractionManagerAccessor) mc.interactionManager).aether$setBreakCooldown(delay.i());
    }
}
