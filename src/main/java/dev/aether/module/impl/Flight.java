package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/** Velocity-based flight. Most servers will kick or flag this. */
public class Flight extends ClientModule {
    private final NumberSetting speed = add(new NumberSetting("Speed", 1.0, 0.1, 5.0, 0.1));
    private final NumberSetting vSpeed = add(new NumberSetting("Vertical Speed", 0.6, 0.1, 3.0, 0.1));

    public Flight() { super("Flight", "Fly with WASD / Space / Shift", Category.MOVEMENT); }

    @Override public void onTick(MinecraftClient mc) {
        if (mc.currentScreen != null || Freecam.active) return;
        var p = mc.player;
        float yaw = p.getYaw() * (float) (Math.PI / 180.0);
        Vec3d fwd = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3d right = new Vec3d(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3d h = Vec3d.ZERO;
        if (mc.options.forwardKey.isPressed()) h = h.add(fwd);
        if (mc.options.backKey.isPressed())    h = h.subtract(fwd);
        if (mc.options.rightKey.isPressed())   h = h.add(right);
        if (mc.options.leftKey.isPressed())    h = h.subtract(right);
        if (h.lengthSquared() > 1e-6) h = h.normalize().multiply(speed.get());
        double y = 0;
        if (mc.options.jumpKey.isPressed()) y += vSpeed.get();
        if (mc.options.sneakKey.isPressed()) y -= vSpeed.get();
        p.setVelocity(h.x, y, h.z);
    }
}
