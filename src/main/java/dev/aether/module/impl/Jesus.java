package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/** Stay on top of water (and optionally lava). Hold sneak to sink. */
public class Jesus extends ClientModule {
    private final BoolSetting lava = add(new BoolSetting("Lava", false));

    public Jesus() { super("Jesus", "Walk on water", Category.MOVEMENT); }

    @Override public void onTick(MinecraftClient mc) {
        var p = mc.player;
        if (mc.options.sneakKey.isPressed()) return;
        if (p.isTouchingWater() || (lava.get() && p.isInLava())) {
            Vec3d v = p.getVelocity();
            p.setVelocity(v.x, Math.max(v.y, 0.11), v.z);
        }
    }
}
