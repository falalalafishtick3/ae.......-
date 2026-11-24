package dev.aether.module.impl;

import dev.aether.module.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/** Action: bind a key to copy your coordinates to the clipboard. */
public class CopyCoords extends ClientModule {
    public CopyCoords() { super("CopyCoords", "Copy your coordinates with one key", Category.WORLD); }

    @Override public void onEnable() {
        enabled = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        String s = (int) Math.floor(mc.player.getX()) + " " + (int) Math.floor(mc.player.getY()) + " " + (int) Math.floor(mc.player.getZ());
        mc.keyboard.setClipboard(s);
        mc.player.sendMessage(Text.literal("[Aether] Copied " + s + " (" + mc.world.getRegistryKey().getValue().getPath() + ")"), false);
    }
}
