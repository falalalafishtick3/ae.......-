package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class Spider extends ClientModule {
    private final NumberSetting speed = add(new NumberSetting("Climb Speed", 0.2, 0.05, 0.6, 0.01));

    public Spider() { super("Spider", "Climb walls by pushing against them", Category.MOVEMENT); }

    @Override public void onTick(MinecraftClient mc) {
        var p = mc.player;
        if (p.horizontalCollision && !p.isTouchingWater()) {
            Vec3d v = p.getVelocity();
            p.setVelocity(v.x, speed.get(), v.z);
        }
    }
}
