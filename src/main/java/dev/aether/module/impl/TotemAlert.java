package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.Items;
import net.minecraft.util.Util;

/** Big warning when you have no totem in your offhand. */
public class TotemAlert extends ClientModule {
    private final BoolSetting onlyIfHave = add(new BoolSetting("Only If I Have Totems", false));
    private final NumberSetting offY = add(new NumberSetting("Offset Y", 40, 0, 200, 1));

    public TotemAlert() { super("TotemAlert", "Warns when your offhand has no totem", Category.HUD); }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        var p = mc.player;
        if (p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        int inv = 0;
        for (int i = 0; i < 36; i++) if (p.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) inv += p.getInventory().getStack(i).getCount();
        if (onlyIfHave.get() && inv == 0) return;
        double pulse = 0.6 + 0.4 * Math.sin(Util.getMeasuringTimeMs() / 180.0);
        String t = inv > 0 ? "NO TOTEM IN OFFHAND  (" + inv + " in inventory)" : "NO TOTEMS";
        var tr = mc.textRenderer;
        var m = g.getMatrices();
        m.pushMatrix();
        m.translate(g.getScaledWindowWidth() / 2f, g.getScaledWindowHeight() / 2f + offY.i());
        m.scale(1.6f, 1.6f);
        g.drawText(tr, t, -tr.getWidth(t) / 2, 0, ((int) (255 * pulse) << 24) | 0xFF4040, true);
        m.popMatrix();
    }
}
