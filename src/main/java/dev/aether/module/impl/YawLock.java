package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;

/** Snaps your view to straight angles (great for walking/tunnelling in a perfect line). */
public class YawLock extends ClientModule {
    private final ModeSetting step = add(new ModeSetting("Yaw Step", "90", "45", "30", "15"));
    private final BoolSetting lockPitch = add(new BoolSetting("Lock Pitch", false));
    private final NumberSetting pitch = add(new NumberSetting("Pitch", 0, -90, 90, 5));
    private final BoolSetting onlyMoving = add(new BoolSetting("Only While Moving", true));

    public YawLock() { super("YawLock", "Snap yaw to fixed angles", Category.MOVEMENT); }

    @Override public void onTick(MinecraftClient mc) {
        if (mc.currentScreen != null || Freecam.active) return;
        var p = mc.player;
        if (onlyMoving.get()) {
            var o = mc.options;
            if (!(o.forwardKey.isPressed() || o.backKey.isPressed() || o.leftKey.isPressed() || o.rightKey.isPressed())) return;
        }
        float s = Float.parseFloat(step.get());
        p.setYaw(Math.round(p.getYaw() / s) * s);
        if (lockPitch.get()) p.setPitch(pitch.f());
    }
}
