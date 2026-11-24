package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.WaypointStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;

/** Compass bar at the top of the screen, with your waypoints as colored markers. */
public class Compass extends ClientModule {
    private final NumberSetting width = add(new NumberSetting("Width", 240, 120, 500, 10));
    private final NumberSetting offY = add(new NumberSetting("Offset Y", 6, 0, 200, 1));
    private final BoolSetting waypoints = add(new BoolSetting("Show Waypoints", true));
    private final BoolSetting degrees = add(new BoolSetting("Show Degrees", true));

    private static final String[] NAMES = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    public Compass() { super("Compass", "Direction bar with waypoint markers", Category.HUD); }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        var tr = mc.textRenderer;
        int w = width.i(), x0 = (g.getScaledWindowWidth() - w) / 2, y0 = offY.i(), cx = x0 + w / 2;
        float yaw = mc.player.getYaw();
        dev.aether.gui.Ui.shadow(g, x0, y0, w, 16, 7, 34);
        dev.aether.gui.Ui.rrect(g, x0 - 1, y0 - 1, w + 2, 18, 8, 0x1FFFFFFF);
        dev.aether.gui.Ui.rrect(g, x0, y0, w, 16, 7, 0xB00D1015);
        dev.aether.gui.Ui.rrect(g, x0 + 10, y0 + 15, w - 20, 1, 0, ClickGuiModule.accent());
        g.enableScissor(x0, y0, x0 + w, y0 + 20);
        for (int i = 0; i < 8; i++) {
            float diff = MathHelper.wrapDegrees(i * 45f - yaw);
            if (Math.abs(diff) > 95) continue;
            int x = cx + (int) (diff / 90f * (w / 2f));
            boolean card = i % 2 == 0;
            g.drawText(tr, NAMES[i], x - tr.getWidth(NAMES[i]) / 2, y0 + 4, card ? 0xFFFFFFFF : 0xFF9AA7B5, card);
        }
        for (int d = 0; d < 360; d += 15) {
            float diff = MathHelper.wrapDegrees(d - yaw);
            if (Math.abs(diff) > 95 || d % 45 == 0) continue;
            int x = cx + (int) (diff / 90f * (w / 2f));
            g.fill(x, y0 + 11, x + 1, y0 + 15, 0x66FFFFFF);
        }
        if (waypoints.get()) {
            String dim = mc.world.getRegistryKey().getValue().getPath();
            for (WaypointStore.Entry e : WaypointStore.all()) {
                if (!e.dim.equals(dim)) continue;
                double dx = e.x + 0.5 - mc.player.getX(), dz = e.z + 0.5 - mc.player.getZ();
                float diff = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz)) - yaw);
                if (Math.abs(diff) > 90) continue;
                int x = cx + (int) (diff / 90f * (w / 2f));
                int col = 0xFF000000 | MathHelper.hsvToRgb(e.hue, 0.6f, 1f);
                g.fill(x - 1, y0 + 1, x + 2, y0 + 9, col);
            }
        }
        g.disableScissor();
        g.fill(cx, y0, cx + 1, y0 + 3, 0xFFFFFFFF);
        if (degrees.get()) {
            String d = String.valueOf((int) ((MathHelper.wrapDegrees(yaw + 180f) + 360f) % 360f));
            g.drawText(tr, d, cx - tr.getWidth(d) / 2, y0 + 18, 0xFFCCCCCC, true);
        }
    }
}
