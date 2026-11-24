package dev.aether.module.impl;

import dev.aether.module.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Jumps automatically at the edge of a block while you run. */
public class Parkour extends ClientModule {
    public Parkour() { super("Parkour", "Auto-jump at ledges", Category.MOVEMENT); }

    @Override public void onTick(MinecraftClient mc) {
        var p = mc.player;
        if (!p.isOnGround() || p.isSneaking() || mc.currentScreen != null) return;
        Vec3d v = p.getVelocity();
        double sp = Math.hypot(v.x, v.z);
        if (sp < 0.05) return;
        BlockPos ahead = BlockPos.ofFloored(p.getX() + v.x / sp * 0.4, p.getY() - 1.0, p.getZ() + v.z / sp * 0.4);
        if (mc.world.getBlockState(ahead).isAir() && mc.world.getBlockState(BlockPos.ofFloored(p.getX(), p.getY() - 1.0, p.getZ())).isAir() == false)
            p.jump();
    }
}
