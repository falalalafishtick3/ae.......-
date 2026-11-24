package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.util.Friends;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;

/** Action: bind a key, look at a player and press it to add/remove them as a friend. */
public class FriendToggle extends ClientModule {
    public FriendToggle() { super("FriendToggle", "Press its key while aiming at a player to (un)friend them", Category.COMBAT); }

    @Override public void onEnable() {
        enabled = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.crosshairTarget instanceof EntityHitResult r && r.getEntity() instanceof PlayerEntity p) {
            String n = p.getName().getString();
            boolean added = Friends.toggle(n);
            mc.player.sendMessage(Text.literal("[Aether] " + (added ? "Added " : "Removed ") + n + (added ? " to friends" : " from friends")), false);
        } else mc.player.sendMessage(Text.literal("[Aether] Aim at a player first"), false);
    }
}
