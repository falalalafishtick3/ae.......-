package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class FastLadder extends ClientModule {
    private final NumberSetting speed = add(new NumberSetting("Climb Speed", 0.3, 0.12, 0.8, 0.01));

    public FastLadder() { super("FastLadder", "Climb ladders and vines faster", Category.MOVEMENT); }

    @Override public void onTick(MinecraftClient mc) {
        var p = mc.player;
        if (p.isClimbing() && p.horizontalCollision && !mc.options.sneakKey.isPressed()) {
            Vec3d v = p.getVelocity();
            p.setVelocity(v.x, speed.get(), v.z);
        }
    }
}
