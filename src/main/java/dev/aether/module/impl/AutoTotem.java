package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.InvUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Keeps a totem in your offhand WITHOUT ghost items:
 *  1) totem in hotbar  -> select it + send the vanilla "swap hands" action (works while moving, no inventory click, no desync)
 *  2) totem only in inventory -> (optionally) click it into the hotbar, but only while you stand still, then step 1.
 */
public class AutoTotem extends ClientModule {
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 2, 0, 10, 1));
    private final BoolSetting inventoryClick = add(new BoolSetting("Pull From Inventory", true));
    private final BoolSetting onlyStill = add(new BoolSetting("Inventory Pull Only When Still", true));
    private int timer;

    public AutoTotem() { super("AutoTotem", "Keeps a totem in your offhand (no ghost items)", Category.COMBAT); }

    private static boolean totem(ItemStack s) { return s.isOf(Items.TOTEM_OF_UNDYING); }

    private void swapHands(MinecraftClient mc, int hotbarSlot) {
        var net = mc.getNetworkHandler();
        var p = mc.player;
        if (net == null) return;
        int prev = InvUtil.selected(mc);
        if (hotbarSlot != prev) { p.getInventory().setSelectedSlot(hotbarSlot); net.sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot)); }
        net.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
        if (hotbarSlot != prev) { p.getInventory().setSelectedSlot(prev); net.sendPacket(new UpdateSelectedSlotC2SPacket(prev)); }
    }

    @Override public void onTick(MinecraftClient mc) {
        if (timer > 0) { timer--; return; }
        var p = mc.player;
        if (totem(p.getOffHandStack()) || mc.currentScreen != null) return;

        int hb = InvUtil.find(mc, AutoTotem::totem);
        if (hb >= 0) { swapHands(mc, hb); timer = delay.i() + 2; return; }

        if (!inventoryClick.get()) return;
        if (onlyStill.get() && !InvUtil.still(mc)) return;
        for (int i = 9; i < 36; i++) {
            if (!totem(p.getInventory().getStack(i))) continue;
            int target = -1;
            for (int h = 0; h < 9; h++) if (p.getInventory().getStack(h).isEmpty()) { target = h; break; }
            if (target < 0) target = InvUtil.selected(mc);
            mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, i, target, SlotActionType.SWAP, p);
            timer = delay.i() + 3;   // next tick the hotbar branch swaps it into the offhand
            return;
        }
    }
}
