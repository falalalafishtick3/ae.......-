package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.InvUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AnchorMacro extends ClientModule {
    private final NumberSetting delay      = add(new NumberSetting("Delay", 2, 0, 10, 1));
    private final NumberSetting charges    = add(new NumberSetting("Charges", 1, 1, 4, 1));
    private final NumberSetting explodeSlot = add(new NumberSetting("Explode Slot (0 = auto)", 0, 0, 9, 1));
    private final BoolSetting autoPlace    = add(new BoolSetting("Auto Place", false));
    private final BoolSetting onlyUseKey   = add(new BoolSetting("Only While Holding Use", true));
    private final BoolSetting switchBack   = add(new BoolSetting("Switch Back", true));
    private final BoolSetting swing        = add(new BoolSetting("Swing", true));

    private int cooldown = 0, prevSlot = -1;
    private BlockPos placed;

    public AnchorMacro() { super("AnchorMacro", "Charge and detonate respawn anchors", Category.COMBAT); }

    @Override public void onDisable() { placed = null; prevSlot = -1; cooldown = 0; }

    @Override public void onTick(MinecraftClient mc) {
        if (cooldown > 0) { cooldown--; return; }
        if (mc.currentScreen != null) return;
        if (onlyUseKey.get() && !mc.options.useKey.isPressed()) { placed = null; return; }

        BlockPos pos = null; Direction face = Direction.UP;
        BlockHitResult bhr = (mc.crosshairTarget instanceof BlockHitResult h && h.getType() == HitResult.Type.BLOCK) ? h : null;
        if (bhr != null && mc.world.getBlockState(bhr.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR)) {
            pos = bhr.getBlockPos(); face = bhr.getSide();
        } else if (placed != null && mc.world.getBlockState(placed).isOf(Blocks.RESPAWN_ANCHOR)) {
            pos = placed;
        }

        if (pos == null) {
            if (autoPlace.get() && bhr != null && mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) {
                InvUtil.useBlock(mc, bhr.getBlockPos(), bhr.getSide(), swing.get());
                placed = bhr.getBlockPos().offset(bhr.getSide());
                cooldown = delay.i();
            }
            return;
        }
        if (mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) > 36) return;

        BlockState st = mc.world.getBlockState(pos);
        int ch = st.get(RespawnAnchorBlock.CHARGES);
        if (prevSlot < 0) prevSlot = InvUtil.selected(mc);

        if (ch < charges.i()) {
            int slot = InvUtil.find(mc, s -> s.isOf(Items.GLOWSTONE));
            if (slot < 0) return;
            InvUtil.select(mc, slot);
            InvUtil.useBlock(mc, pos, face, swing.get());
        } else {
            int slot = explodeSlot.i() > 0 ? explodeSlot.i() - 1
                    : InvUtil.find(mc, s -> s.isOf(Items.TOTEM_OF_UNDYING));
            if (slot < 0) slot = InvUtil.find(mc, s -> !s.isOf(Items.GLOWSTONE));
            InvUtil.select(mc, slot);
            InvUtil.useBlock(mc, pos, face, swing.get());
            placed = null;
            if (switchBack.get() && prevSlot >= 0) InvUtil.select(mc, prevSlot);
            prevSlot = -1;
        }
        cooldown = delay.i();
    }
}
