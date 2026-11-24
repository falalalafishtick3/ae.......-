package dev.aether.module.impl;

import dev.aether.mixin.MinecraftClientAccessor;
import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;

public class FastPlace extends ClientModule {
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 0, 0, 4, 1));

    public FastPlace() { super("FastPlace", "Removes the right-click delay", Category.WORLD); }

    @Override public void onTick(MinecraftClient mc) {
        ((MinecraftClientAccessor) mc).aether$setItemUseCooldown(delay.i());
    }
}
