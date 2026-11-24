package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import org.lwjgl.glfw.GLFW;

/** Look around with the mouse without turning your character. Default: hold Left Alt. */
public class Freelook extends ClientModule {
    public static boolean active;
    public static float yaw, pitch;

    private final BoolSetting thirdPerson = add(new BoolSetting("Third Person", false));
    private Perspective prev;

    public Freelook() {
        super("Freelook", "Hold a key to look around freely", Category.MOVEMENT);
        key = GLFW.GLFW_KEY_LEFT_ALT;
        bindMode.set("Hold");
    }

    private void start(MinecraftClient mc) {
        if (mc.player == null) return;
        yaw = mc.player.getYaw(); pitch = mc.player.getPitch();
        active = true;
        if (thirdPerson.get()) { prev = mc.options.getPerspective(); mc.options.setPerspective(Perspective.THIRD_PERSON_BACK); }
    }

    @Override public void onEnable() { start(MinecraftClient.getInstance()); }

    @Override public void onDisable() {
        active = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (prev != null) { mc.options.setPerspective(prev); prev = null; }
    }

    @Override public void onTick(MinecraftClient mc) { if (!active) start(mc); }
}
