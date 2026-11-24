package dev.aether.module.impl;

import dev.aether.module.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.text.Text;

public class DeathCoords extends ClientModule {
    private boolean said;

    public DeathCoords() { super("DeathCoords", "Prints where you died", Category.WORLD); }

    @Override public void onTick(MinecraftClient mc) {
        if (mc.currentScreen instanceof DeathScreen) {
            if (!said) {
                said = true;
                var p = mc.player;
                p.sendMessage(Text.literal(String.format("[Aether] You died at %d, %d, %d (%s)", (int) p.getX(), (int) p.getY(), (int) p.getZ(),
                        mc.world.getRegistryKey().getValue().getPath())), false);
            }
        } else said = false;
    }
}
