package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import java.util.LinkedHashMap;
import java.util.Map;

/** Drops junk items from your inventory while you mine. */
public class AutoDrop extends ClientModule {
    private final Map<BoolSetting, Item[]> groups = new LinkedHashMap<>();
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 4, 1, 20, 1));
    private final BoolSetting hotbar = add(new BoolSetting("Include Hotbar", false));
    private int t;

    private void group(String name, boolean def, Item... items) { groups.put(add(new BoolSetting(name, def)), items); }

    public AutoDrop() {
        super("AutoDrop", "Drops junk items automatically", Category.WORLD);
        group("Cobble / Stone", true, Items.COBBLESTONE, Items.COBBLED_DEEPSLATE, Items.STONE, Items.DEEPSLATE);
        group("Dirt / Gravel", true, Items.DIRT, Items.COARSE_DIRT, Items.GRAVEL);
        group("Granite / Diorite / Andesite", true, Items.GRANITE, Items.DIORITE, Items.ANDESITE, Items.TUFF);
        group("Netherrack", false, Items.NETHERRACK, Items.BLACKSTONE, Items.BASALT);
        group("Mob Junk", false, Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.BONE, Items.STRING, Items.POISONOUS_POTATO);
    }

    private boolean junk(ItemStack s) {
        if (s.isEmpty()) return false;
        for (var e : groups.entrySet()) if (e.getKey().get()) for (Item it : e.getValue()) if (s.isOf(it)) return true;
        return false;
    }

    @Override public void onTick(MinecraftClient mc) {
        if (++t % delay.i() != 0 || mc.currentScreen != null) return;
        if (ClickGuiModule.safeClicks() && !dev.aether.util.InvUtil.still(mc)) return;
        var p = mc.player;
        for (int i = hotbar.get() ? 0 : 9; i < 36; i++) {
            if (!junk(p.getInventory().getStack(i))) continue;
            int slot = i < 9 ? i + 36 : i;
            mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, slot, 1, SlotActionType.THROW, p);
            return;
        }
    }
}
