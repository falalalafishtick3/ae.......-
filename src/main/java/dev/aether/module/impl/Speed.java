package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/** Horizontal speed boost. Servers with movement checks will flag this. */
public class Speed extends ClientModule {
    private final NumberSetting speed = add(new NumberSetting("Speed", 0.35, 0.1, 1.5, 0.01));
    private final BoolSetting autoJump = add(new BoolSetting("Auto Jump", false));

    public Speed() { super("Speed", "Move faster on the ground and in air", Category.MOVEMENT); }

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
        if (h.lengthSquared() < 1e-6) return;
        if (autoJump.get() && p.isOnGround()) p.jump();
        h = h.normalize().multiply(speed.get());
        Vec3d v = p.getVelocity();
        p.setVelocity(h.x, v.y, h.z);
    }
}
