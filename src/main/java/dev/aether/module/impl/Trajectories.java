package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.render.EspRenderer;
import dev.aether.setting.*;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;

/** Predicts the flight path of ender pearls, snowballs, eggs, potions, XP bottles and drawn bows. */
public class Trajectories extends ClientModule {
    private final NumberSetting steps = add(new NumberSetting("Max Steps", 120, 30, 300, 10));
    private final NumberSetting width = add(new NumberSetting("Line Width", 2, 1, 5, 0.5));
    private final BoolSetting throughWalls = add(new BoolSetting("Through Walls", true));
    private final ColorSetting color = add(new ColorSetting("Color", 0.12f, 0.8f, 1f));

    public Trajectories() { super("Trajectories", "Shows where thrown items / arrows will land", Category.RENDER); }

    /** @return {speed, gravity} or null if the stack isn't a projectile. */
    private double[] profile(MinecraftClient mc, ItemStack s) {
        if (s.isOf(Items.ENDER_PEARL) || s.isOf(Items.SNOWBALL) || s.isOf(Items.EGG)) return new double[]{1.5, 0.03};
        if (s.isOf(Items.SPLASH_POTION) || s.isOf(Items.LINGERING_POTION)) return new double[]{0.5, 0.05};
        if (s.isOf(Items.EXPERIENCE_BOTTLE)) return new double[]{0.7, 0.07};
        if (s.isOf(Items.BOW) && mc.player.isUsingItem() && mc.player.getActiveItem().isOf(Items.BOW)) {
            float f = BowItem.getPullProgress(mc.player.getItemUseTime());
            return f < 0.1f ? null : new double[]{f * 3.0, 0.05};
        }
        return null;
    }

    @Override public void onRender(WorldRenderContext ctx, MinecraftClient mc) {
        var p = mc.player;
        double[] prof = profile(mc, p.getMainHandStack());
        if (prof == null) prof = profile(mc, p.getOffHandStack());
        if (prof == null) return;

        Vec3d cam = ctx.worldState().cameraRenderState.pos;
        Vec3d pos = p.getEyePos().add(0, -0.1, 0);
        Vec3d vel = p.getRotationVec(1.0f).multiply(prof[0]);
        List<Vec3d> pts = new ArrayList<>();
        pts.add(pos.subtract(cam));
        Vec3d hit = null;
        for (int i = 0; i < steps.i(); i++) {
            pos = pos.add(vel);
            vel = vel.multiply(0.99).add(0, -prof[1], 0);
            BlockPos bp = BlockPos.ofFloored(pos);
            if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) { hit = pos; pts.add(pos.subtract(cam)); break; }
            pts.add(pos.subtract(cam));
            if (pos.y < mc.world.getBottomY() - 8) break;
        }
        int argb = 0xFF000000 | color.rgb();
        EspRenderer.polyline(ctx.matrices(), ctx.consumers(), pts, argb, width.f(), throughWalls.get());
        if (hit != null) {
            Vec3d h = hit.subtract(cam);
            java.util.List<EspRenderer.Item> it = new ArrayList<>();
            it.add(new EspRenderer.Item(new Box(h.x - 0.25, h.y - 0.25, h.z - 0.25, h.x + 0.25, h.y + 0.25, h.z + 0.25), color.rgb(), 255, 70));
            EspRenderer.fills(ctx.matrices(), ctx.consumers(), it, throughWalls.get());
            EspRenderer.outlines(ctx.matrices(), ctx.consumers(), it, throughWalls.get(), width.f());
        }
    }
}
