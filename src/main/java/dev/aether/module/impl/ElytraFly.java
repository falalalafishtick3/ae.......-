package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/** Controlled elytra flight. W/S = forward/back along your view, Space = up, Shift = hover. */
public class ElytraFly extends ClientModule {
    private final NumberSetting speed = add(new NumberSetting("Speed", 1.5, 0.3, 4.0, 0.1));
    private final NumberSetting up = add(new NumberSetting("Vertical Speed", 0.6, 0.1, 2.0, 0.1));

    public ElytraFly() { super("ElytraFly", "Control your elytra with the keys", Category.MOVEMENT); }

    @Override public void onTick(MinecraftClient mc) {
        var p = mc.player;
        if (!p.isGliding() || mc.currentScreen != null) return;
        Vec3d look = p.getRotationVec(1.0f);
        boolean f = mc.options.forwardKey.isPressed(), b = mc.options.backKey.isPressed();
        boolean j = mc.options.jumpKey.isPressed(), s = mc.options.sneakKey.isPressed();
        if (s) { p.setVelocity(0, 0, 0); return; }
        if (!f && !b && !j) return;
        Vec3d v = Vec3d.ZERO;
        if (f) v = v.add(look.multiply(speed.get()));
        if (b) v = v.subtract(look.multiply(speed.get()));
        if (j) v = v.add(0, up.get(), 0);
        p.setVelocity(v);
    }
}
