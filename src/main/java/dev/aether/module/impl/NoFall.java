package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

/** Tells the server you're on the ground while falling. Many anticheats detect this. */
public class NoFall extends ClientModule {
    private final NumberSetting minFall = add(new NumberSetting("Min Fall Distance", 2.5, 1, 10, 0.5));

    public NoFall() { super("NoFall", "Cancels fall damage", Category.MOVEMENT); }

    @Override public void onTick(MinecraftClient mc) {
        var p = mc.player;
        if (p.fallDistance > minFall.get() && !p.isOnGround() && !p.isGliding() && mc.getNetworkHandler() != null)
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, p.horizontalCollision));
    }
}
