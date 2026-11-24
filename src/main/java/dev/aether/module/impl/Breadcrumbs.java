package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.render.EspRenderer;
import dev.aether.setting.*;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;

/** Draws the path you walked so you can retrace your steps. */
public class Breadcrumbs extends ClientModule {
    private final NumberSetting maxPoints = add(new NumberSetting("Max Points", 1500, 100, 8000, 100));
    private final NumberSetting spacing = add(new NumberSetting("Point Spacing", 1.0, 0.3, 4.0, 0.1));
    private final NumberSetting width = add(new NumberSetting("Line Width", 2, 1, 5, 0.5));
    private final BoolSetting throughWalls = add(new BoolSetting("Through Walls", true));
    private final ColorSetting color = add(new ColorSetting("Color", 0.55f, 0.55f, 1f));

    private final List<Vec3d> pts = new ArrayList<>();
    private String dim = "";

    public Breadcrumbs() { super("Breadcrumbs", "Trail of where you have been", Category.RENDER); }

    @Override public void onDisable() { synchronized (pts) { pts.clear(); } }

    @Override public void onTick(MinecraftClient mc) {
        String d = mc.world.getRegistryKey().getValue().getPath();
        Vec3d now = new Vec3d(mc.player.getX(), mc.player.getY() + 0.1, mc.player.getZ());
        synchronized (pts) {
            if (!d.equals(dim)) { pts.clear(); dim = d; }
            if (pts.isEmpty() || pts.get(pts.size() - 1).distanceTo(now) >= spacing.get()) {
                pts.add(now);
                while (pts.size() > maxPoints.i()) pts.remove(0);
            }
        }
    }

    @Override public void onRender(WorldRenderContext ctx, MinecraftClient mc) {
        Vec3d cam = ctx.worldState().cameraRenderState.pos;
        List<Vec3d> rel = new ArrayList<>();
        synchronized (pts) { for (Vec3d v : pts) rel.add(v.subtract(cam)); }
        EspRenderer.polyline(ctx.matrices(), ctx.consumers(), rel, 0xFF000000 | color.rgb(), width.f(), throughWalls.get());
    }
}
