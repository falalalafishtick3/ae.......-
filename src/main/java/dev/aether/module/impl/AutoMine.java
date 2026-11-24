package dev.aether.module.impl;

import dev.aether.module.*;
import net.minecraft.client.MinecraftClient;

/** Holds the attack/mine key for you. */
public class AutoMine extends ClientModule {
    public AutoMine() { super("AutoMine", "Holds left click for you", Category.WORLD); }
    @Override public void onTick(MinecraftClient mc) { if (mc.currentScreen == null) mc.options.attackKey.setPressed(true); }
    @Override public void onDisable() { MinecraftClient.getInstance().options.attackKey.setPressed(false); }
}
