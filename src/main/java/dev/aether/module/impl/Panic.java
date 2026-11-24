package dev.aether.module.impl;

import dev.aether.module.*;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/** Action: turns every module off at once (default key: Right Ctrl). */
public class Panic extends ClientModule {
    public Panic() { super("Panic", "Disable everything instantly", Category.CLIENT); key = GLFW.GLFW_KEY_RIGHT_CONTROL; }

    @Override public void onEnable() {
        enabled = false;
        for (ClientModule m : ModuleManager.MODULES) if (m instanceof Notifications && m.enabled) m.setEnabled(false);
        for (ClientModule m : ModuleManager.MODULES) if (m != this && m.category != Category.CLIENT && m.enabled) m.setEnabled(false);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof dev.aether.gui.ClickGuiScreen) mc.setScreen(null);
    }
}
