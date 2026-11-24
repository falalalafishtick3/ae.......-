package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;

/** Narrow FOV zoom. Bind it to a key (middle-click it in the GUI) and toggle. */
public class Zoom extends ClientModule {
    private final NumberSetting zoomFov = add(new NumberSetting("Zoom FOV", 30, 30, 70, 1));
    private Integer orig;

    public Zoom() { super("Zoom", "Zoom in like a spyglass", Category.RENDER); key = org.lwjgl.glfw.GLFW.GLFW_KEY_C; bindMode.set("Hold"); }

    @Override public void onTick(MinecraftClient mc) {
        if (orig == null) orig = mc.options.getFov().getValue();
        if (mc.options.getFov().getValue() != zoomFov.i()) mc.options.getFov().setValue(zoomFov.i());
    }
    @Override public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (orig != null) { mc.options.getFov().setValue(orig); orig = null; }
    }
}
