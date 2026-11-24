package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;

public class AutoRespawn extends ClientModule {
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 10, 0, 60, 1));
    private int t;

    public AutoRespawn() { super("AutoRespawn", "Respawns automatically", Category.WORLD); }

    @Override public void onTick(MinecraftClient mc) {
        if (!(mc.currentScreen instanceof DeathScreen)) { t = 0; return; }
        if (++t >= delay.i()) { mc.player.requestRespawn(); t = 0; }
    }
}
