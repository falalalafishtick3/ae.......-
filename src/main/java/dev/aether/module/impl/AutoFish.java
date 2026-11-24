package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoFish extends ClientModule {
    private final BoolSetting recast = add(new BoolSetting("Recast", true));
    private final NumberSetting sens = add(new NumberSetting("Bite Sensitivity", 0.10, 0.04, 0.4, 0.01));
    private int age, cooldown;
    private double lastY;

    public AutoFish() { super("AutoFish", "Reels in fish and recasts (hold a fishing rod)", Category.WORLD); }

    @Override public void onTick(MinecraftClient mc) {
        var p = mc.player;
        if (!p.getMainHandStack().isOf(Items.FISHING_ROD)) return;
        if (cooldown > 0) { cooldown--; return; }
        FishingBobberEntity hook = p.fishHook;
        if (hook == null) {
            age = 0;
            if (recast.get()) { mc.interactionManager.interactItem(p, Hand.MAIN_HAND); p.swingHand(Hand.MAIN_HAND); cooldown = 20; }
            return;
        }
        age++;
        double dy = hook.getY() - lastY;
        lastY = hook.getY();
        if (age > 40 && hook.isTouchingWater() && dy < -sens.get()) {
            mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
            p.swingHand(Hand.MAIN_HAND);
            cooldown = 15; age = 0;
        }
    }
}
