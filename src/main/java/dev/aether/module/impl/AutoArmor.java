package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;

/** Equips the best armor piece for any empty armor slot. */
public class AutoArmor extends ClientModule {
    private static final String[] TIERS = {"netherite", "diamond", "iron", "chainmail", "golden", "turtle", "leather"};
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] SUFFIX = {"_helmet", "_chestplate", "_leggings", "_boots"};

    private final NumberSetting delay = add(new NumberSetting("Delay", 3, 0, 20, 1));
    private int timer;

    public AutoArmor() { super("AutoArmor", "Auto-equips armor into empty slots", Category.COMBAT); }

    private static int score(ItemStack s, String suffix) {
        if (s.isEmpty()) return -1;
        String id = Registries.ITEM.getId(s.getItem()).getPath();
        if (!id.endsWith(suffix)) return -1;
        for (int i = 0; i < TIERS.length; i++) if (id.startsWith(TIERS[i])) return TIERS.length - i;
        return 0;
    }

    @Override public void onTick(MinecraftClient mc) {
        if (timer > 0) { timer--; return; }
        if (mc.currentScreen != null) return;
        if (ClickGuiModule.safeClicks() && !dev.aether.util.InvUtil.still(mc)) return;
        var p = mc.player;
        for (int a = 0; a < 4; a++) {
            if (!p.getEquippedStack(SLOTS[a]).isEmpty()) continue;
            int best = -1, bestScore = -1;
            for (int i = 0; i < 36; i++) {
                int sc = score(p.getInventory().getStack(i), SUFFIX[a]);
                if (sc > bestScore) { bestScore = sc; best = i; }
            }
            if (best < 0) continue;
            int slot = best < 9 ? best + 36 : best;
            mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, slot, 0, SlotActionType.QUICK_MOVE, p);
            timer = delay.i();
            return;
        }
    }
}
