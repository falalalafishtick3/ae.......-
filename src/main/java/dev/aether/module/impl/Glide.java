package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class Glide extends ClientModule {
    private final NumberSetting fall = add(new NumberSetting("Fall Speed", 0.1, 0.02, 0.5, 0.01));

    public Glide() { super("Glide", "Fall slowly", Category.MOVEMENT); }

    @Override public void onTick(MinecraftClient mc) {
        var p = mc.player;
        if (p.isOnGround() || p.isTouchingWater() || p.isInLava() || p.isGliding()) return;
        Vec3d v = p.getVelocity();
        if (v.y < -fall.get()) p.setVelocity(v.x, -fall.get(), v.z);
    }
}
