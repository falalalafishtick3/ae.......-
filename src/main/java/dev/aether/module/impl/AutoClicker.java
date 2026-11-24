package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import java.util.Random;

/** Holds left click => attacks the entity under your crosshair at a randomized CPS. */
public class AutoClicker extends ClientModule {
    private final NumberSetting minCps = add(new NumberSetting("Min CPS", 8, 1, 20, 1));
    private final NumberSetting maxCps = add(new NumberSetting("Max CPS", 12, 1, 20, 1));
    private final BoolSetting cooldown = add(new BoolSetting("Respect Cooldown", true));
    private final BoolSetting onlyWeapon = add(new BoolSetting("Only On Entities", true));

    private final Random rng = new Random();
    private long next;

    public AutoClicker() { super("AutoClicker", "Clicks for you while you hold attack", Category.COMBAT); }

    @Override public void onTick(MinecraftClient mc) {
        if (mc.currentScreen != null || !mc.options.attackKey.isPressed()) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult r) || !(r.getEntity() instanceof LivingEntity t) || !t.isAlive()) return;
        long now = System.currentTimeMillis();
        if (now < next) return;
        if (cooldown.get() && mc.player.getAttackCooldownProgress(0f) < 0.9f) return;
        mc.interactionManager.attackEntity(mc.player, t);
        mc.player.swingHand(Hand.MAIN_HAND);
        double lo = Math.min(minCps.get(), maxCps.get()), hi = Math.max(minCps.get(), maxCps.get());
        double cps = lo + rng.nextDouble() * (hi - lo);
        next = now + (long) (1000.0 / cps);
    }
}
