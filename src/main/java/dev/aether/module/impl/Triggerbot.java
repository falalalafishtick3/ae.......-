package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

public class Triggerbot extends ClientModule {
    private final NumberSetting cooldown = add(new NumberSetting("Cooldown %", 95, 10, 100, 1));
    private final BoolSetting players  = add(new BoolSetting("Players", true));
    private final BoolSetting hostiles = add(new BoolSetting("Hostiles", true));
    private final BoolSetting passives = add(new BoolSetting("Passives", false));
    private final BoolSetting critOnly = add(new BoolSetting("Crits Only", false));

    public Triggerbot() { super("Triggerbot", "Attacks whatever your crosshair is on", Category.COMBAT); }

    @Override public void onTick(MinecraftClient mc) {
        if (mc.currentScreen != null) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult r) || !(r.getEntity() instanceof LivingEntity t) || !t.isAlive()) return;
        if (t instanceof PlayerEntity pe && dev.aether.util.Friends.is(pe.getName().getString())) return;
        boolean ok = (t instanceof PlayerEntity && players.get()) || (t instanceof Monster && hostiles.get())
                || (t instanceof PassiveEntity && passives.get());
        if (!ok) return;
        var p = mc.player;
        if (p.getAttackCooldownProgress(0f) < cooldown.f() / 100f) return;
        if (critOnly.get() && (p.isOnGround() || p.fallDistance <= 0)) return;
        mc.interactionManager.attackEntity(p, t);
        p.swingHand(Hand.MAIN_HAND);
    }
}
