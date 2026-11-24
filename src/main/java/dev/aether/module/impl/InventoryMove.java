package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

/** Keep walking / jumping / sprinting while your inventory (or a container) is open. */
public class InventoryMove extends ClientModule {
    private final BoolSetting onlyInv = add(new BoolSetting("Only Player Inventory", false));

    public InventoryMove() { super("InventoryMove", "Move while inventories are open", Category.MOVEMENT); }

    private void sync(MinecraftClient mc, KeyBinding k) {
        try {
            int code = InputUtil.fromTranslationKey(k.getBoundKeyTranslationKey()).getCode();
            k.setPressed(InputUtil.isKeyPressed(mc.getWindow(), code));
        } catch (Throwable ignored) {}
    }

    @Override public void onTick(MinecraftClient mc) {
        if (!(mc.currentScreen instanceof HandledScreen<?>)) return;
        if (onlyInv.get() && !(mc.currentScreen instanceof InventoryScreen)) return;
        var o = mc.options;
        sync(mc, o.forwardKey); sync(mc, o.backKey); sync(mc, o.leftKey); sync(mc, o.rightKey); sync(mc, o.jumpKey); sync(mc, o.sprintKey);
    }
}
