package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

/** Sends a tiny fake hop right before each attack so it lands as a critical (hook: AttackMixin). */
public class Criticals extends ClientModule {
    public static Criticals INSTANCE;
    private final NumberSetting hop = add(new NumberSetting("Hop Height", 0.0625, 0.01, 0.2, 0.005));

    public Criticals() { super("Criticals", "Makes ground attacks critical", Category.COMBAT); INSTANCE = this; }

    public void apply(MinecraftClient mc) {
        var p = mc.player;
        if (p == null || mc.getNetworkHandler() == null || !p.isOnGround() || p.isTouchingWater() || p.isInLava() || p.isClimbing()) return;
        double x = p.getX(), y = p.getY(), z = p.getZ();
        boolean hc = p.horizontalCollision;
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + hop.get(), z, false, hc));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, hc));
    }
}
