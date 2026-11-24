package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.InvUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/** Switches to the fastest hotbar tool for the block you are breaking. */
public class AutoTool extends ClientModule {
    private final BoolSetting switchBack = add(new BoolSetting("Switch Back", true));
    private int prev = -1;

    public AutoTool() { super("AutoTool", "Picks the best tool automatically", Category.WORLD); }

    @Override public void onTick(MinecraftClient mc) {
        boolean breaking = mc.currentScreen == null && mc.options.attackKey.isPressed()
                && mc.crosshairTarget instanceof BlockHitResult h && h.getType() == HitResult.Type.BLOCK;
        if (!breaking) {
            if (prev >= 0 && switchBack.get()) InvUtil.select(mc, prev);
            prev = -1;
            return;
        }
        BlockState st = mc.world.getBlockState(((BlockHitResult) mc.crosshairTarget).getBlockPos());
        int best = InvUtil.selected(mc); float bestSpeed = mc.player.getInventory().getStack(best).getMiningSpeedMultiplier(st);
        for (int i = 0; i < 9; i++) {
            float sp = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(st);
            if (sp > bestSpeed + 0.01f) { bestSpeed = sp; best = i; }
        }
        if (best != InvUtil.selected(mc)) { if (prev < 0) prev = InvUtil.selected(mc); InvUtil.select(mc, best); }
    }
}
