package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.InvUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class CrystalMacro extends ClientModule {
    private final BoolSetting place        = add(new BoolSetting("Place", true));
    private final BoolSetting breakC       = add(new BoolSetting("Break", true));
    private final NumberSetting placeDelay = add(new NumberSetting("Place Delay", 1, 0, 10, 1));
    private final NumberSetting breakDelay = add(new NumberSetting("Break Delay", 0, 0, 10, 1));
    private final BoolSetting autoSwitch   = add(new BoolSetting("Auto Switch To Crystal", true));
    private final BoolSetting onlyUseKey   = add(new BoolSetting("Only While Holding Use", true));
    private final BoolSetting swing        = add(new BoolSetting("Swing", true));

    private int placeTimer, breakTimer;

    public CrystalMacro() { super("CrystalMacro", "Auto place/break end crystals at your crosshair", Category.COMBAT); }

    @Override public void onTick(MinecraftClient mc) {
        if (placeTimer > 0) placeTimer--;
        if (breakTimer > 0) breakTimer--;
        if (mc.currentScreen != null) return;
        if (onlyUseKey.get() && !mc.options.useKey.isPressed()) return;

        if (autoSwitch.get() && !mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) {
            InvUtil.select(mc, InvUtil.find(mc, s -> s.isOf(Items.END_CRYSTAL)));
        }

        HitResult hit = mc.crosshairTarget;
        if (hit == null) return;

        // Break
        if (breakC.get() && breakTimer == 0 && hit instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof EndCrystalEntity crystal) {
            mc.interactionManager.attackEntity(mc.player, crystal);
            if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
            breakTimer = breakDelay.i();
            return;
        }

        // Place
        if (place.get() && placeTimer == 0 && mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)
                && hit instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bhr.getBlockPos();
            BlockState st = mc.world.getBlockState(pos);
            Block b = st.getBlock();
            if ((b == Blocks.OBSIDIAN || b == Blocks.BEDROCK)
                    && mc.world.isAir(pos.up()) && mc.world.isAir(pos.up(2))
                    && mc.world.getOtherEntities(null, new Box(pos.up()).stretch(0, 1, 0)).isEmpty()) {
                InvUtil.useBlock(mc, pos, Direction.UP, swing.get());
                placeTimer = placeDelay.i();
            }
        }
    }
}
