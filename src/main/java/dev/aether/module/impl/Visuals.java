package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.render.EspRenderer;
import dev.aether.setting.*;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;

public class Visuals extends ClientModule {
    private final BoolSetting fullbright   = add(new BoolSetting("Fullbright", true));
    private final BoolSetting customFov    = add(new BoolSetting("Custom FOV", false));
    private final NumberSetting fov        = add(new NumberSetting("FOV", 90, 30, 110, 1));

    private final BoolSetting esp          = add(new BoolSetting("Entity ESP", true));
    private final BoolSetting players      = add(new BoolSetting("Players", true));
    private final BoolSetting hostiles     = add(new BoolSetting("Hostiles", true));
    private final BoolSetting passives     = add(new BoolSetting("Passives", false));
    private final BoolSetting itemsB       = add(new BoolSetting("Dropped Items", false));
    private final BoolSetting crystals     = add(new BoolSetting("End Crystals", true));
    private final ModeSetting renderMode   = add(new ModeSetting("Render Mode", "Both", "Outline", "Fill"));
    private final NumberSetting outlineOp  = add(new NumberSetting("Outline Opacity", 100, 0, 100, 1));
    private final NumberSetting fillOp     = add(new NumberSetting("Fill Opacity", 20, 0, 100, 1));
    private final NumberSetting lineWidth  = add(new NumberSetting("Line Width", 2, 1, 6, 0.5));
    private final NumberSetting range      = add(new NumberSetting("Range", 96, 16, 256, 8));
    private final BoolSetting throughWalls = add(new BoolSetting("Through Walls", true));
    private final BoolSetting tracers      = add(new BoolSetting("Tracers", false));
    private final ColorSetting cPlayer     = add(new ColorSetting("Player", 0.00f, 0.85f, 1f));
    private final ColorSetting cHostile    = add(new ColorSetting("Hostile", 0.08f, 0.9f, 1f));
    private final ColorSetting cPassive    = add(new ColorSetting("Passive", 0.33f, 0.8f, 1f));
    private final ColorSetting cItem       = add(new ColorSetting("Item", 0.15f, 0.7f, 1f));
    private final ColorSetting cCrystal    = add(new ColorSetting("Crystal", 0.85f, 0.8f, 1f));
    private final ColorSetting cFriend     = add(new ColorSetting("Friend", 0.33f, 0.7f, 1f));
    private final BoolSetting projectiles = add(new BoolSetting("Projectiles (pearls/arrows)", false));
    private final ColorSetting cProj       = add(new ColorSetting("Projectile", 0.55f, 0.7f, 1f));
    private final BoolSetting minecarts    = add(new BoolSetting("Chest Minecarts", false));
    private final ColorSetting cCart       = add(new ColorSetting("Minecart", 0.12f, 0.8f, 1f));

    private boolean nvApplied;
    private Integer origFov;

    public Visuals() {
        super("Visuals", "Fullbright, FOV and entity ESP", Category.RENDER);
        fov.visibleWhen(customFov::get);
        for (Setting<?> s : new Setting<?>[]{players, hostiles, passives, itemsB, crystals, renderMode, outlineOp, fillOp,
                lineWidth, range, throughWalls, tracers, cPlayer, cHostile, cPassive, cItem, cCrystal, cFriend, projectiles, cProj, minecarts, cCart})
            s.visibleWhen(esp::get);
        outlineOp.visibleWhen(() -> esp.get() && !renderMode.is("Fill"));
        fillOp.visibleWhen(() -> esp.get() && !renderMode.is("Outline"));
        cPlayer.visibleWhen(() -> esp.get() && players.get());
        cHostile.visibleWhen(() -> esp.get() && hostiles.get());
        cPassive.visibleWhen(() -> esp.get() && passives.get());
        cItem.visibleWhen(() -> esp.get() && itemsB.get());
        cCrystal.visibleWhen(() -> esp.get() && crystals.get());
    }

    private void removeNv(MinecraftClient mc) {
        if (nvApplied && mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        nvApplied = false;
    }
    private void restoreFov(MinecraftClient mc) {
        if (origFov != null) { mc.options.getFov().setValue(origFov); origFov = null; }
    }

    @Override public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        removeNv(mc); restoreFov(mc);
    }

    @Override public void onTick(MinecraftClient mc) {
        if (fullbright.get()) {
            if (!mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,
                        StatusEffectInstance.INFINITE, 0, false, false, false));
                nvApplied = true;
            }
        } else removeNv(mc);

        if (customFov.get()) {
            if (origFov == null) origFov = mc.options.getFov().getValue();
            if (mc.options.getFov().getValue() != fov.i()) mc.options.getFov().setValue(fov.i());
        } else restoreFov(mc);
    }

    private ColorSetting colorFor(Entity e) {
        if (e instanceof PlayerEntity pe) {
            if (!players.get()) return null;
            return dev.aether.util.Friends.is(pe.getName().getString()) ? cFriend : cPlayer;
        }
        if (e instanceof net.minecraft.entity.projectile.ProjectileEntity) return projectiles.get() ? cProj : null;
        if (e instanceof net.minecraft.entity.vehicle.ChestMinecartEntity) return minecarts.get() ? cCart : null;
        if (e instanceof EndCrystalEntity) return crystals.get() ? cCrystal : null;
        if (e instanceof Monster) return hostiles.get() ? cHostile : null;
        if (e instanceof PassiveEntity) return passives.get() ? cPassive : null;
        if (e instanceof ItemEntity) return itemsB.get() ? cItem : null;
        return null;
    }

    @Override public void onRender(WorldRenderContext ctx, MinecraftClient mc) {
        if (!esp.get()) return;
        float tp = mc.getRenderTickCounter().getTickProgress(false);
        Vec3d cam = ctx.worldState().cameraRenderState.pos;
        double r = range.get();
        boolean outline = !renderMode.is("Fill"), fill = !renderMode.is("Outline");
        int oa = Math.round(outlineOp.f() / 100f * 255f), fa = Math.round(fillOp.f() / 100f * 255f);

        List<EspRenderer.Item> items = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || e.isInvisible() && !(e instanceof PlayerEntity)) continue;
            ColorSetting c = colorFor(e);
            if (c == null) continue;
            Vec3d lp = e.getLerpedPos(tp);
            if (lp.distanceTo(cam) > r) continue;
            double w = e.getWidth() / 2.0, h = e.getHeight();
            Box b = new Box(lp.x - cam.x - w, lp.y - cam.y, lp.z - cam.z - w, lp.x - cam.x + w, lp.y - cam.y + h, lp.z - cam.z + w);
            items.add(new EspRenderer.Item(b, c.rgb(), oa, fa));
        }
        if (items.isEmpty()) return;

        MatrixStack m = ctx.matrices();
        VertexConsumerProvider buf = ctx.consumers();
        if (fill) EspRenderer.fills(m, buf, items, throughWalls.get());
        if (outline) EspRenderer.outlines(m, buf, items, throughWalls.get(), lineWidth.f());
        if (tracers.get())
            EspRenderer.tracers(m, buf, mc.player.getRotationVec(1.0f).multiply(0.6), items, throughWalls.get(), lineWidth.f());
    }
}
