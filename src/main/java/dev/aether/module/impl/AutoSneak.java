package dev.aether.module.impl;

import dev.aether.module.*;
import net.minecraft.client.MinecraftClient;

public class AutoSneak extends ClientModule {
    public AutoSneak() { super("AutoSneak", "Holds sneak for you", Category.MOVEMENT); }
    @Override public void onTick(MinecraftClient mc) { if (mc.currentScreen == null) mc.options.sneakKey.setPressed(true); }
    @Override public void onDisable() { MinecraftClient.getInstance().options.sneakKey.setPressed(false); }
}
