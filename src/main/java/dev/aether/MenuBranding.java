package dev.aether;

import dev.aether.gui.Ui;
import dev.aether.module.impl.ClickGuiModule;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

/** Animated "AETHER CLIENT" badge on the main menu. */
public final class MenuBranding {
    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof TitleScreen) ScreenEvents.afterRender(screen).register((s, g, mx, my, d) -> draw(g, s.width));
        });
    }

    private static void draw(DrawContext g, int sw) {
        var tr = MinecraftClient.getInstance().textRenderer;
        double t = Util.getMeasuringTimeMs() / 1000.0;
        String name = "AETHER", sub = "CLIENT";
        int nw = tr.getWidth(name) * 2 + 10, subw = tr.getWidth(sub) + 12, ver = tr.getWidth("1.21.11") + 8;
        int w = nw + subw + ver + 18, h = 26, x = 10, y = 10;
        int ac = ClickGuiModule.accent();
        Ui.shadow(g, x, y, w, h, 8, 70);
        Ui.rrect(g, x - 1, y - 1, w + 2, h + 2, 9, Ui.withAlpha(ac & 0xFFFFFF, 0x55));
        Ui.rrect(g, x, y, w, h, 8, 0xE60B0E13);
        // animated gradient text
        int cx = x + 9;
        var m = g.getMatrices();
        for (int i = 0; i < name.length(); i++) {
            String ch = String.valueOf(name.charAt(i));
            float hue = ClickGuiModule.baseHue() + 0.01f + 0.12f * (float) (0.5 + 0.5 * Math.sin(t * 1.4 + i * 0.7));
            int col = 0xFF000000 | MathHelper.hsvToRgb(hue, ClickGuiModule.sat(), 1f);
            m.pushMatrix(); m.translate(cx, y + 6); m.scale(2f, 2f);
            g.drawText(tr, ch, 0, 0, col, true);
            m.popMatrix();
            cx += tr.getWidth(ch) * 2;
        }
        int sx = x + 9 + nw;
        Ui.rrect(g, sx, y + 7, 1, h - 14, 0, 0x33FFFFFF);
        g.drawText(tr, sub, sx + 7, y + 6, Ui.lighten(ac, 0.35f), false);
        g.drawText(tr, "winter edition", sx + 7, y + 16, 0xFF7D8694, false);
        g.drawText(tr, "1.21.11", x + w - ver + 2, y + 9, 0x88FFFFFF, false);
    }
}
