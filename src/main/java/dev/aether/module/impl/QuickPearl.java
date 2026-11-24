package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.util.InvUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

/** Action: bind a key; pressing it switches to the item, uses it, and switches back. */
public class QuickPearl extends ClientModule {
    public QuickPearl() { super("QuickPearl", "Throw an ender pearl with one key", Category.COMBAT); }

    @Override public void onEnable() {
        enabled = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;
        int slot = InvUtil.find(mc, s -> s.isOf(Items.ENDER_PEARL));
        if (slot < 0) { mc.player.sendMessage(Text.literal("[Aether] QuickPearl: none in hotbar"), false); return; }
        int prev = InvUtil.selected(mc);
        InvUtil.select(mc, slot);
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);
        InvUtil.select(mc, prev);
    }
}
