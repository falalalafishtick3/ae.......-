package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.Friends;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;

/** Raises your shield when an enemy gets close. */
public class AutoBlock extends ClientModule {
    private final NumberSetting range = add(new NumberSetting("Range", 4.5, 2, 8, 0.5));
    private final BoolSetting players = add(new BoolSetting("Players", true));
    private final BoolSetting hostiles = add(new BoolSetting("Hostiles", true));
    private boolean holding;

    public AutoBlock() { super("AutoBlock", "Auto-raise shield when enemies are near", Category.COMBAT); }

    @Override public void onDisable() { if (holding) { MinecraftClient.getInstance().options.useKey.setPressed(false); holding = false; } }

    @Override public void onTick(MinecraftClient mc) {
        var p = mc.player;
        boolean shield = p.getOffHandStack().isOf(Items.SHIELD) || p.getMainHandStack().isOf(Items.SHIELD);
        boolean threat = false;
        if (shield && mc.currentScreen == null) {
            for (Entity e : mc.world.getEntities()) {
                if (e == p || p.distanceTo(e) > range.get()) continue;
                if (e instanceof PlayerEntity o && players.get() && !Friends.is(o.getName().getString()) && !o.isSpectator()) { threat = true; break; }
                if (e instanceof Monster && hostiles.get()) { threat = true; break; }
            }
        }
        if (threat && !holding) { mc.options.useKey.setPressed(true); holding = true; }
        else if (!threat && holding) { mc.options.useKey.setPressed(false); holding = false; }
    }
}
