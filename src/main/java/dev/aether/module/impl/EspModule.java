package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.render.EspRenderer;
import dev.aether.setting.*;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.*;
import java.util.ArrayList;
import java.util.List;

/** Shared settings + rendering for modules that highlight a list of boxes. Subclasses implement scan(). */
public abstract class EspModule extends ClientModule {
    public record Target(Box box, int rgb) {}

    protected ModeSetting renderMode, colorMode;
    protected NumberSetting lineWidth, outlineOpacity, fillOpacity, range, speed, scanInterval;
    protected BoolSetting throughWalls, fadeWithDistance, tracers;

    private volatile List<Target> targets = List.of();
    private int tick;

    protected EspModule(String n, String d, Category c) { super(n, d, c); }

    protected void addCommonSettings() {
        renderMode       = add(new ModeSetting("Render Mode", "Both", "Outline", "Fill"));
        outlineOpacity   = add(new NumberSetting("Outline Opacity", 100, 0, 100, 1));
        lineWidth        = add(new NumberSetting("Line Width", 2, 1, 6, 0.5));
        fillOpacity      = add(new NumberSetting("Fill Opacity", 25, 0, 100, 1));
        colorMode        = add(new ModeSetting("Color Mode", "Custom", "Rainbow", "Pulse", "Distance"));
        speed            = add(new NumberSetting("Effect Speed", 1, 0.1, 5, 0.1));
        throughWalls     = add(new BoolSetting("Through Walls", true));
        tracers          = add(new BoolSetting("Tracers", false));
        fadeWithDistance = add(new BoolSetting("Fade With Distance", false));
        range            = add(new NumberSetting("Range", 128, 16, 512, 8));
        scanInterval     = add(new NumberSetting("Scan Interval", 20, 1, 40, 1));

        outlineOpacity.visibleWhen(() -> !renderMode.is("Fill"));
        lineWidth.visibleWhen(() -> !renderMode.is("Fill") || tracers.get());
        fillOpacity.visibleWhen(() -> !renderMode.is("Outline"));
        speed.visibleWhen(() -> colorMode.is("Rainbow") || colorMode.is("Pulse"));
    }

    /** Fill {@code out} with the boxes to highlight. Called every scanInterval ticks. */
    protected abstract void scan(MinecraftClient mc, List<Target> out);

    @Override public void onDisable() { targets = List.of(); tick = 0; }

    @Override public void onTick(MinecraftClient mc) {
        if (tick++ % scanInterval.i() != 0) return;
        List<Target> out = new ArrayList<>();
        scan(mc, out);
        targets = out;
    }

    @Override public void onRender(WorldRenderContext ctx, MinecraftClient mc) {
        List<Target> list = targets;
        if (list.isEmpty()) return;

        Vec3d cam = ctx.worldState().cameraRenderState.pos;
        MatrixStack m = ctx.matrices();
        VertexConsumerProvider buf = ctx.consumers();

        boolean outline = !renderMode.is("Fill"), fill = !renderMode.is("Outline");
        long t = Util.getMeasuringTimeMs();
        double r = range.get();

        List<EspRenderer.Item> items = new ArrayList<>();
        for (Target tg : list) {
            Vec3d c = tg.box().getCenter();
            double d = Math.sqrt(c.squaredDistanceTo(cam));
            if (d > r + 24) continue;

            int rgb = tg.rgb();
            float mul = 1f;
            switch (colorMode.get()) {
                case "Rainbow" -> rgb = MathHelper.hsvToRgb(
                        (float) (((t * 0.0002 * speed.get()) + (c.x + c.z) * 0.01) % 1.0), 0.8f, 1f);
                case "Pulse" -> mul = (float) (0.35 + 0.65 * (0.5 + 0.5 * Math.sin(t * 0.004 * speed.get())));
                case "Distance" -> rgb = MathHelper.hsvToRgb((float) (0.33 * (1 - Math.min(1, d / r))), 0.9f, 1f);
                default -> {}
            }
            if (fadeWithDistance.get()) mul *= (float) Math.max(0.15, 1 - 0.75 * d / r);

            int oa = Math.round(outlineOpacity.f() / 100f * 255f * mul);
            int fa = Math.round(fillOpacity.f() / 100f * 255f * mul);
            items.add(new EspRenderer.Item(tg.box().offset(-cam.x, -cam.y, -cam.z), rgb & 0xFFFFFF, oa, fa));
        }

        if (fill) EspRenderer.fills(m, buf, items, throughWalls.get());
        if (outline) EspRenderer.outlines(m, buf, items, throughWalls.get(), lineWidth.f());
        if (tracers.get()) {
            Vec3d start = mc.player.getRotationVec(1.0f).multiply(0.6);
            EspRenderer.tracers(m, buf, start, items, throughWalls.get(), lineWidth.f());
        }
    }
}
