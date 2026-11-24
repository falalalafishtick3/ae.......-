package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/** Shift-clicks everything out of an open chest/barrel/shulker. */
public class ChestStealer extends ClientModule {
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 2, 0, 10, 1));
    private final BoolSetting autoClose = add(new BoolSetting("Auto Close When Empty", true));
    private int timer;

    public ChestStealer() { super("ChestStealer", "Quickly takes all items from open containers", Category.WORLD); }

    @Override public void onTick(MinecraftClient mc) {
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler h)) { timer = 0; return; }
        if (timer-- > 0) return;
        int n = h.getRows() * 9;
        for (int i = 0; i < n; i++) {
            if (!h.getSlot(i).getStack().isEmpty()) {
                mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                timer = delay.i();
                return;
            }
        }
        if (autoClose.get()) mc.player.closeHandledScreen();
    }
}
