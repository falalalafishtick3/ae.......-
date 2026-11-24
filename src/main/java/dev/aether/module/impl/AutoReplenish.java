package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

/** Refills hotbar stacks that run low from your main inventory. */
public class AutoReplenish extends ClientModule {
    private final NumberSetting threshold = add(new NumberSetting("Refill At (items left)", 8, 1, 32, 1));
    private final NumberSetting delay = add(new NumberSetting("Check Every (ticks)", 10, 2, 40, 1));
    private int t;

    public AutoReplenish() { super("AutoReplenish", "Refills low hotbar stacks", Category.WORLD); }

    @Override public void onTick(MinecraftClient mc) {
        if (++t % delay.i() != 0 || mc.currentScreen != null) return;
        if (ClickGuiModule.safeClicks() && !dev.aether.util.InvUtil.still(mc)) return;
        var p = mc.player;
        for (int i = 0; i < 9; i++) {
            ItemStack h = p.getInventory().getStack(i);
            if (h.isEmpty() || h.getMaxCount() <= 1 || h.getCount() > threshold.i()) continue;
            for (int j = 9; j < 36; j++) {
                ItemStack s = p.getInventory().getStack(j);
                if (!s.isEmpty() && s.isOf(h.getItem()) && s.getCount() > h.getCount()) {
                    mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, j, i, SlotActionType.SWAP, p);
                    return;
                }
            }
        }
    }
}
