package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;

public class AutoWalk extends ClientModule {
    private final BoolSetting sprint = add(new BoolSetting("Sprint", false));

    public AutoWalk() { super("AutoWalk", "Walks forward for you", Category.MOVEMENT); }

    @Override public void onTick(MinecraftClient mc) {
        if (mc.currentScreen != null) return;
        mc.options.forwardKey.setPressed(true);
        if (sprint.get()) mc.player.setSprinting(true);
    }
    @Override public void onDisable() { MinecraftClient.getInstance().options.forwardKey.setPressed(false); }
}
