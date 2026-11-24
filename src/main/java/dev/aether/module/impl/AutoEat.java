package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.InvUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class AutoEat extends ClientModule {
    private final NumberSetting threshold = add(new NumberSetting("Eat Below Hunger", 14, 1, 19, 1));
    private final BoolSetting avoidGapples = add(new BoolSetting("Skip Golden Apples", true));
    private final BoolSetting switchBack = add(new BoolSetting("Switch Back", true));

    private boolean eating; private int prevSlot = -1, ticks;

    public AutoEat() { super("AutoEat", "Eats food when you get hungry", Category.COMBAT); }

    private boolean edible(ItemStack s) {
        if (!s.contains(DataComponentTypes.FOOD)) return false;
        if (s.isOf(Items.CHORUS_FRUIT) || s.isOf(Items.ROTTEN_FLESH) || s.isOf(Items.SPIDER_EYE) || s.isOf(Items.POISONOUS_POTATO)) return false;
        return !(avoidGapples.get() && (s.isOf(Items.GOLDEN_APPLE) || s.isOf(Items.ENCHANTED_GOLDEN_APPLE)));
    }

    private void stop(MinecraftClient mc) {
        mc.options.useKey.setPressed(false);
        if (switchBack.get() && prevSlot >= 0) InvUtil.select(mc, prevSlot);
        eating = false; prevSlot = -1; ticks = 0;
    }

    @Override public void onDisable() { MinecraftClient mc = MinecraftClient.getInstance(); if (eating && mc.player != null) stop(mc); }

    @Override public void onTick(MinecraftClient mc) {
        var p = mc.player;
        int food = p.getHungerManager().getFoodLevel();
        if (eating) {
            ticks++;
            if (ticks > 8 && !p.isUsingItem()) stop(mc);
            else if (food >= 20) stop(mc);
            return;
        }
        if (mc.currentScreen != null || p.isUsingItem() || food > threshold.i()) return;
        int slot = InvUtil.find(mc, this::edible);
        if (slot < 0) return;
        prevSlot = InvUtil.selected(mc);
        InvUtil.select(mc, slot);
        mc.options.useKey.setPressed(true);
        eating = true; ticks = 0;
    }
}
