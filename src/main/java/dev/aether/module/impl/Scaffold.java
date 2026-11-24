package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.InvUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Places blocks under your feet as you walk. */
public class Scaffold extends ClientModule {
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 1, 0, 6, 1));
    private final BoolSetting swing = add(new BoolSetting("Swing", true));
    private final BoolSetting switchBack = add(new BoolSetting("Switch Back", true));
    private int timer, prev = -1;

    public Scaffold() { super("Scaffold", "Auto-places blocks beneath you", Category.MOVEMENT); }

    @Override public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (prev >= 0 && mc.player != null) InvUtil.select(mc, prev);
        prev = -1;
    }

    @Override public void onTick(MinecraftClient mc) {
        if (timer > 0) { timer--; return; }
        if (mc.currentScreen != null) return;
        var p = mc.player;
        BlockPos below = BlockPos.ofFloored(p.getX(), p.getY() - 1.0, p.getZ());
        if (!mc.world.getBlockState(below).isAir()) {
            if (prev >= 0 && switchBack.get()) { InvUtil.select(mc, prev); prev = -1; }
            return;
        }
        int slot = p.getMainHandStack().getItem() instanceof BlockItem ? InvUtil.selected(mc)
                : InvUtil.find(mc, s -> s.getItem() instanceof BlockItem);
        if (slot < 0) return;
        for (Direction d : Direction.values()) {
            BlockPos n = below.offset(d);
            if (mc.world.getBlockState(n).isAir()) continue;
            if (prev < 0 && slot != InvUtil.selected(mc)) prev = InvUtil.selected(mc);
            InvUtil.select(mc, slot);
            InvUtil.useBlock(mc, n, d.getOpposite(), swing.get());
            timer = delay.i();
            return;
        }
    }
}
