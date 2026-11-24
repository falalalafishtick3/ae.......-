package dev.aether.module.impl;

import dev.aether.hud.HudUtil;
import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.FoundBases;
import dev.aether.util.WaypointStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** Long-range map of found bases and waypoints (dots clamp to the edge when out of range). */
public class BaseMap extends ClientModule {
    private final ModeSetting corner = add(new ModeSetting("Position", "Top Right", "Top Left", "Bottom Left", "Bottom Right"));
    private final NumberSetting offX = add(new NumberSetting("Offset X", 6, 0, 400, 1));
    private final NumberSetting offY = add(new NumberSetting("Offset Y", 40, 0, 400, 1));
    private final NumberSetting size = add(new NumberSetting("Size", 110, 60, 220, 2));
    private final NumberSetting range = add(new NumberSetting("Range (blocks)", 2000, 100, 20000, 100));
    private final BoolSetting rotate = add(new BoolSetting("Rotate With View", false));
    private final BoolSetting bases = add(new BoolSetting("Show Bases", true));
    private final BoolSetting wps = add(new BoolSetting("Show Waypoints", true));

    public BaseMap() { super("BaseMap", "Map of found bases and waypoints", Category.HUD); }

    private static String dimNow(MinecraftClient mc) { return mc.world.getRegistryKey().getValue().getPath(); }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int sz = size.i();
        int[] p = HudUtil.pos(corner.get(), offX.i(), offY.i(), sz, sz, g.getScaledWindowWidth(), g.getScaledWindowHeight());
        int cx = p[0] + sz / 2, cy = p[1] + sz / 2;
        dev.aether.gui.Ui.shadow(g, p[0], p[1], sz, sz, 6, 36);
        dev.aether.gui.Ui.rrect(g, p[0] - 1, p[1] - 1, sz + 2, sz + 2, 7, 0x22FFFFFF);
        dev.aether.gui.Ui.rrect(g, p[0], p[1], sz, sz, 6, 0xB00D1015);
        g.fill(cx, p[1], cx + 1, p[1] + sz, 0x26FFFFFF);
        g.fill(p[0], cy, p[0] + sz, cy + 1, 0x26FFFFFF);

        double yaw = Math.toRadians(mc.player.getYaw());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        double k = (sz / 2.0 - 4) / range.get();
        String dim = dimNow(mc);
        String server = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "singleplayer";

        if (bases.get())
            for (FoundBases.Entry e : FoundBases.all()) {
                if (!e.dim.equals(dim) || !e.server.equals(server)) continue;
                dot(g, cx, cy, sz, e.x + 0.5 - mc.player.getX(), e.z + 0.5 - mc.player.getZ(), k, fx, fz, rx, rz, 0xFFFFC857);
            }
        if (wps.get())
            for (WaypointStore.Entry e : WaypointStore.all()) {
                if (!e.dim.equals(dim)) continue;
                int col = 0xFF000000 | net.minecraft.util.math.MathHelper.hsvToRgb(e.hue, 0.6f, 1f);
                dot(g, cx, cy, sz, e.x + 0.5 - mc.player.getX(), e.z + 0.5 - mc.player.getZ(), k, fx, fz, rx, rz, col);
            }
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
    }

    private void dot(DrawContext g, int cx, int cy, int sz, double dx, double dz, double k,
                     double fx, double fz, double rx, double rz, int col) {
        double sx, sy;
        if (rotate.get()) { sx = (dx * rx + dz * rz) * k; sy = -(dx * fx + dz * fz) * k; }
        else { sx = dx * k; sy = dz * k; }
        double lim = sz / 2.0 - 3;
        boolean clamped = false;
        double m = Math.max(Math.abs(sx), Math.abs(sy));
        if (m > lim) { sx = sx / m * lim; sy = sy / m * lim; clamped = true; }
        int x = cx + (int) sx, y = cy + (int) sy;
        int s = clamped ? 1 : 2;
        g.fill(x - s, y - s, x + s + 1, y + s + 1, clamped ? (col & 0x00FFFFFF) | 0x99000000 : col);
    }
}
