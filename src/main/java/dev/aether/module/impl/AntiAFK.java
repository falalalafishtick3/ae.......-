package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import java.util.Random;

public class AntiAFK extends ClientModule {
    private final NumberSetting interval = add(new NumberSetting("Interval (s)", 20, 5, 120, 1));
    private final BoolSetting jump = add(new BoolSetting("Jump", true));
    private final BoolSetting look = add(new BoolSetting("Look Around", true));
    private final BoolSetting swing = add(new BoolSetting("Swing Arm", true));

    private final Random rng = new Random();
    private int timer;

    public AntiAFK() { super("AntiAFK", "Tiny actions so you don't get kicked for idling", Category.MOVEMENT); }

    @Override public void onTick(MinecraftClient mc) {
        if (++timer < interval.i() * 20) return;
        timer = 0;
        var p = mc.player;
        int pick = rng.nextInt(3);
        if (pick == 0 && jump.get() && p.isOnGround()) p.jump();
        else if (pick == 1 && look.get()) { p.setYaw(p.getYaw() + rng.nextInt(61) - 30); p.setPitch(Math.max(-60, Math.min(60, p.getPitch() + rng.nextInt(21) - 10))); }
        else if (swing.get()) p.swingHand(Hand.MAIN_HAND);
    }
}
