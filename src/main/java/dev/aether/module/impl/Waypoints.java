package dev.aether.module.impl;

import dev.aether.hud.HudUtil;
import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.WaypointStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Shows saved waypoints in the world (ESP) and as a distance list on the HUD. Add them from the Hub screen or the AddWaypoint key. */
public class Waypoints extends EspModule {
    private final BoolSetting list = add(new BoolSetting("Distance List", true));
    private final NumberSetting listCount = add(new NumberSetting("List Size", 5, 1, 12, 1));
    private final ModeSetting corner = add(new ModeSetting("List Position", "Top Left", "Top Right", "Bottom Left", "Bottom Right"));
    private final NumberSetting offX = add(new NumberSetting("List Offset X", 6, 0, 300, 1));
    private final NumberSetting offY = add(new NumberSetting("List Offset Y", 120, 0, 300, 1));

    public Waypoints() {
        super("Waypoints", "Saved locations in the world + distance list", Category.RENDER);
        addCommonSettings();
        scanInterval.set(10.0);
        range.set(512.0);
        renderMode.set("Outline");
    }

    private static String dimNow(MinecraftClient mc) { return mc.world.getRegistryKey().getValue().getPath(); }

    @Override protected void scan(MinecraftClient mc, List<Target> out) {
        String dim = dimNow(mc);
        for (WaypointStore.Entry e : WaypointStore.all()) {
            if (!e.dim.equals(dim)) continue;
            out.add(new Target(new Box(e.x + 0.2, e.y, e.z + 0.2, e.x + 0.8, e.y + 2.5, e.z + 0.8),
                    MathHelper.hsvToRgb(e.hue, 0.6f, 1f) & 0xFFFFFF));
        }
    }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        if (!list.get()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        String dim = dimNow(mc);
        List<WaypointStore.Entry> near = new ArrayList<>();
        for (WaypointStore.Entry e : WaypointStore.all()) if (e.dim.equals(dim)) near.add(e);
        if (near.isEmpty()) return;
        near.sort(Comparator.comparingDouble(e -> Math.hypot(e.x - mc.player.getX(), e.z - mc.player.getZ())));
        var tr = mc.textRenderer;
        int n = Math.min(listCount.i(), near.size());
        int w = 0;
        String[] lines = new String[n];
        for (int i = 0; i < n; i++) {
            WaypointStore.Entry e = near.get(i);
            lines[i] = e.name + "  " + (int) Math.hypot(e.x - mc.player.getX(), e.z - mc.player.getZ()) + "m";
            w = Math.max(w, tr.getWidth(lines[i]));
        }
        int[] p = HudUtil.pos(corner.get(), offX.i(), offY.i(), w + 10, n * 10 + 4, g.getScaledWindowWidth(), g.getScaledWindowHeight());
        HudUtil.card(g, p[0], p[1], w + 10, n * 10 + 4, ClickGuiModule.accent());
        for (int i = 0; i < n; i++) {
            int col = 0xFF000000 | MathHelper.hsvToRgb(near.get(i).hue, 0.6f, 1f);
            g.fill(p[0], p[1] + 3 + i * 10, p[0] + 2, p[1] + 11 + i * 10, col);
            g.drawText(tr, lines[i], p[0] + 6, p[1] + 3 + i * 10, 0xFFFFFFFF, true);
        }
    }
}
