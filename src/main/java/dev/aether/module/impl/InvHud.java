package dev.aether.module.impl;

import dev.aether.hud.HudUtil;
import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;

/** Shows your main inventory (27 slots) on screen. */
public class InvHud extends ClientModule {
    private final ModeSetting corner = add(new ModeSetting("Position", "Bottom Right", "Top Left", "Top Right", "Bottom Left"));
    private final NumberSetting offX = add(new NumberSetting("Offset X", 8, 0, 400, 1));
    private final NumberSetting offY = add(new NumberSetting("Offset Y", 8, 0, 400, 1));
    private final NumberSetting bg = add(new NumberSetting("Background Opacity", 60, 0, 100, 1));

    public InvHud() { super("InvHud", "Inventory on your HUD", Category.HUD); }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = 9 * 18 + 6, h = 3 * 18 + 6;
        int[] p = HudUtil.pos(corner.get(), offX.i(), offY.i(), w, h, g.getScaledWindowWidth(), g.getScaledWindowHeight());
        dev.aether.gui.Ui.shadow(g, p[0], p[1], w, h, 5, 34);
        dev.aether.gui.Ui.rrect(g, p[0], p[1], w, h, 5, HudUtil.a(0xFF0D1015, bg.f() / 100f));
        dev.aether.gui.Ui.rrect(g, p[0] + 3, p[1], w - 6, 2, 1, ClickGuiModule.accent());
        for (int i = 0; i < 27; i++) {
            ItemStack st = mc.player.getInventory().getStack(9 + i);
            if (st.isEmpty()) continue;
            int x = p[0] + 3 + (i % 9) * 18, y = p[1] + 3 + (i / 9) * 18;
            g.drawItem(st, x, y);
            g.drawStackOverlay(mc.textRenderer, st, x, y);
        }
    }
}
