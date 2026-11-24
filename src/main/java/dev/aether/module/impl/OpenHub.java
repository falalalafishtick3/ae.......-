package dev.aether.module.impl;

import dev.aether.gui.HubScreen;
import dev.aether.module.*;
import net.minecraft.client.MinecraftClient;

/** Action: opens the Found Bases / Waypoints hub. */
public class OpenHub extends ClientModule {
    public OpenHub() { super("OpenHub", "Open Found Bases + Waypoints", Category.CLIENT); }
    @Override public void onEnable() { enabled = false; MinecraftClient.getInstance().setScreen(new HubScreen(0)); }
}
