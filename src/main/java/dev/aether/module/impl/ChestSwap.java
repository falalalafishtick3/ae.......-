package dev.aether.module.impl;

import dev.aether.module.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

/** Action: bind a key to swap between your elytra and your best chestplate. */
public class ChestSwap extends ClientModule {
    private static final String[] TIERS = {"netherite", "diamond", "iron", "chainmail", "golden", "turtle", "leather"};

    public ChestSwap() { super("ChestSwap", "Swap elytra <-> chestplate with one key", Category.MOVEMENT); }

    private static int plateScore(ItemStack s) {
        if (s.isEmpty()) return -1;
        String id = Registries.ITEM.getId(s.getItem()).getPath();
        if (!id.endsWith("_chestplate")) return -1;
        for (int i = 0; i < TIERS.length; i++) if (id.startsWith(TIERS[i])) return TIERS.length - i;
        return 0;
    }

    @Override public void onEnable() {
        enabled = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        var p = mc.player;
        if (p == null || mc.interactionManager == null) return;
        boolean wearingElytra = p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
        int best = -1, bestScore = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack s = p.getInventory().getStack(i);
            int sc = wearingElytra ? plateScore(s) : (s.isOf(Items.ELYTRA) ? 1 : -1);
            if (sc > bestScore) { bestScore = sc; best = i; }
        }
        if (best < 0) { p.sendMessage(Text.literal("[Aether] ChestSwap: no " + (wearingElytra ? "chestplate" : "elytra") + " in inventory"), false); return; }
        int sync = p.playerScreenHandler.syncId, slot = best < 9 ? best + 36 : best;
        if (best < 9) mc.interactionManager.clickSlot(sync, 6, best, SlotActionType.SWAP, p);
        else {
            mc.interactionManager.clickSlot(sync, slot, 0, SlotActionType.PICKUP, p);
            mc.interactionManager.clickSlot(sync, 6, 0, SlotActionType.PICKUP, p);
            mc.interactionManager.clickSlot(sync, slot, 0, SlotActionType.PICKUP, p);
        }
    }
}
