package dev.aether.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import java.util.function.Predicate;

public final class InvUtil {
    /** @return hotbar slot (0-8) of first matching stack, or -1. */
    public static int find(MinecraftClient mc, Predicate<ItemStack> p) {
        for (int i = 0; i < 9; i++) if (p.test(mc.player.getInventory().getStack(i))) return i;
        return -1;
    }
    /** True when you are (almost) standing still. Inventory clicks while moving get rejected by many anticheats -> ghost items. */
    public static boolean still(MinecraftClient mc) {
        var v = mc.player.getVelocity();
        return Math.hypot(v.x, v.z) < 0.03;
    }
    public static int selected(MinecraftClient mc) { return mc.player.getInventory().getSelectedSlot(); }
    public static void select(MinecraftClient mc, int slot) {
        if (slot >= 0 && slot < 9 && slot != selected(mc)) mc.player.getInventory().setSelectedSlot(slot);
    }
    /** Right-click a block on the given face (main hand). */
    public static void useBlock(MinecraftClient mc, BlockPos pos, Direction face, boolean swing) {
        Vec3d hit = Vec3d.ofCenter(pos).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
        if (swing) mc.player.swingHand(Hand.MAIN_HAND);
    }
}
