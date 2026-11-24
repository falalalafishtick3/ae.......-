package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Auto-sneaks at ledges so you don't walk off. */
public class SafeWalk extends ClientModule {
    private final NumberSetting look = add(new NumberSetting("Look-Ahead", 0.35, 0.1, 1.0, 0.05));
    private boolean sneaking;

    public SafeWalk() { super("SafeWalk", "Stops you walking off edges", Category.MOVEMENT); }

    @Override public void onDisable() { if (sneaking) { MinecraftClient.getInstance().options.sneakKey.setPressed(false); sneaking = false; } }

    @Override public void onTick(MinecraftClient mc) {
        var p = mc.player;
        Vec3d v = p.getVelocity();
        boolean edge = false;
        if (p.isOnGround() && (Math.abs(v.x) > 0.003 || Math.abs(v.z) > 0.003)) {
            double k = look.get() / Math.max(0.05, Math.hypot(v.x, v.z));
            BlockPos ahead = BlockPos.ofFloored(p.getX() + v.x * k, p.getY() - 1.0, p.getZ() + v.z * k);
            edge = mc.world.getBlockState(ahead).isAir() && mc.world.getBlockState(ahead.down()).isAir();
        }
        if (edge && !mc.options.jumpKey.isPressed()) { mc.options.sneakKey.setPressed(true); sneaking = true; }
        else if (sneaking) { mc.options.sneakKey.setPressed(false); sneaking = false; }
    }
}
