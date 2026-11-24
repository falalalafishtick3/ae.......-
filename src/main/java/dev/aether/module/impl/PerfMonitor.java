package dev.aether.module.impl;

import dev.aether.hud.HudUtil;
import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.Perf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import java.util.ArrayList;
import java.util.List;

/** Shows which Aether modules cost the most time (ms per tick / per frame). Turn on if your FPS is low. */
public class PerfMonitor extends ClientModule {
    private final NumberSetting rows = add(new NumberSetting("Rows", 6, 3, 15, 1));
    private final ModeSetting corner = add(new ModeSetting("Position", "Top Right", "Top Left", "Bottom Left", "Bottom Right"));
    private final NumberSetting offX = add(new NumberSetting("Offset X", 6, 0, 400, 1));
    private final NumberSetting offY = add(new NumberSetting("Offset Y", 60, 0, 400, 1));

    public PerfMonitor() { super("PerfMonitor", "Shows what costs FPS", Category.CLIENT); }

    @Override public void onEnable() { Perf.on = true; }
    @Override public void onDisable() { Perf.on = false; }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        Perf.on = true;
        MinecraftClient mc = MinecraftClient.getInstance();
        var tr = mc.textRenderer;
        int n = ModuleManager.MODULES.size();
        if (Perf.avgMs[0].length != n) return;
        // cost per second ~ tick ms * 20 + (render+hud) ms * fps
        double fps = Math.max(1, mc.getCurrentFps());
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < n; i++) idx.add(i);
        idx.sort((a, b) -> Double.compare(cost(b, fps), cost(a, fps)));

        List<String> lines = new ArrayList<>();
        lines.add("FPS " + mc.getCurrentFps() + "   (ms: tick / render / hud)");
        for (int k = 0; k < Math.min(rows.i(), n); k++) {
            int i = idx.get(k);
            if (cost(i, fps) < 1) break;
            lines.add(String.format("%-14s %.2f / %.2f / %.2f", ModuleManager.MODULES.get(i).name,
                    Perf.avgMs[Perf.T][i], Perf.avgMs[Perf.R][i], Perf.avgMs[Perf.H][i]));
        }
        int w = 0; for (String l : lines) w = Math.max(w, tr.getWidth(l));
        int[] p = HudUtil.pos(corner.get(), offX.i(), offY.i(), w + 10, lines.size() * 10 + 4, g.getScaledWindowWidth(), g.getScaledWindowHeight());
        g.fill(p[0], p[1], p[0] + w + 10, p[1] + lines.size() * 10 + 4, 0xA0000000);
        int y = p[1] + 3;
        for (String l : lines) { g.drawText(tr, l, p[0] + 5, y, 0xFFFFFFFF, true); y += 10; }
    }

    private static double cost(int i, double fps) {
        return Perf.avgMs[Perf.T][i] * 20 + (Perf.avgMs[Perf.R][i] + Perf.avgMs[Perf.H][i]) * fps;
    }
}
