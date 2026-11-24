package dev.aether.module.impl;

import dev.aether.hud.HudUtil;
import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ActiveMods extends ClientModule {
    private final ModeSetting corner  = add(new ModeSetting("Position", "Top Right", "Top Left", "Bottom Left", "Bottom Right"));
    private final NumberSetting offX  = add(new NumberSetting("Offset X", 4, 0, 300, 1));
    private final NumberSetting offY  = add(new NumberSetting("Offset Y", 4, 0, 300, 1));
    private final BoolSetting rainbow = add(new BoolSetting("Animated Colors", true));
    private final BoolSetting bg      = add(new BoolSetting("Background", true));
    private final BoolSetting bar     = add(new BoolSetting("Side Bar", true));

    public ActiveMods() { super("ActiveMods", "Lists your enabled modules", Category.HUD); }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        var tr = MinecraftClient.getInstance().textRenderer;
        List<ClientModule> list = new ArrayList<>();
        for (ClientModule m : ModuleManager.MODULES) if (m.enabled && m.category != Category.CLIENT && m.category != Category.HUD) list.add(m);
        list.sort(Comparator.comparingInt((ClientModule m) -> -tr.getWidth(m.name)));
        if (list.isEmpty()) return;

        int sw = g.getScaledWindowWidth(), sh = g.getScaledWindowHeight();
        int rowH = 11, h = list.size() * rowH;
        boolean right = corner.get().endsWith("Right");
        int[] p = HudUtil.pos(corner.get(), offX.i(), offY.i(), 0, h, sw, sh);
        double t = Util.getMeasuringTimeMs() / 1000.0;
        int y = p[1];
        for (int i = 0; i < list.size(); i++) {
            String n = list.get(i).name;
            int w = tr.getWidth(n);
            int x = right ? sw - offX.i() - w - 6 : offX.i();
            int col = rainbow.get()
                    ? 0xFF000000 | MathHelper.hsvToRgb((float) (ClickGuiModule.baseHue() + 0.05 * Math.sin(t * 1.2 + i * 0.5)), ClickGuiModule.sat(), 1f)
                    : ClickGuiModule.accent();
            if (bg.get()) dev.aether.gui.Ui.rrect(g, x - 2, y, w + 6, rowH, 3, 0xA00D1015);
            if (bar.get()) { int bx = right ? x + w + 4 : x - 3; g.fill(bx, y, bx + 1, y + rowH, col); }
            g.drawText(tr, n, x + 1, y + 2, col, true);
            y += rowH;
        }
    }
}
