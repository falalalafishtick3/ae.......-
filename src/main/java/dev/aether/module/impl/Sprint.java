package dev.aether.module.impl;

import dev.aether.module.*;
import net.minecraft.client.MinecraftClient;

public class Sprint extends ClientModule {
    public Sprint() { super("Sprint", "Always sprint while moving forward", Category.MOVEMENT); }

    @Override public void onTick(MinecraftClient mc) {
        var p = mc.player;
        if (mc.currentScreen == null && mc.options.forwardKey.isPressed() && !mc.options.sneakKey.isPressed()
                && !p.isUsingItem() && p.getHungerManager().getFoodLevel() > 6)
            p.setSprinting(true);
    }
}
