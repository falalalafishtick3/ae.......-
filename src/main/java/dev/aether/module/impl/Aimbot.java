package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Aimbot extends ClientModule {
    private final NumberSetting range     = add(new NumberSetting("Range", 4.5, 2, 8, 0.1));
    private final NumberSetting fov       = add(new NumberSetting("FOV", 90, 10, 360, 5));
    private final NumberSetting smooth    = add(new NumberSetting("Smoothness", 5, 1, 20, 0.5));
    private final NumberSetting maxSpeed  = add(new NumberSetting("Max Speed", 30, 1, 180, 1));
    private final ModeSetting aimAt       = add(new ModeSetting("Aim At", "Head", "Body", "Legs"));
    private final BoolSetting players     = add(new BoolSetting("Players", true));
    private final BoolSetting hostiles    = add(new BoolSetting("Hostiles", true));
    private final BoolSetting passives    = add(new BoolSetting("Passives", false));
    private final BoolSetting needLos     = add(new BoolSetting("Require Line Of Sight", true));
    private final ModeSetting activate    = add(new ModeSetting("Activation", "Hold Attack", "Hold Use", "Always"));
    private final BoolSetting sticky      = add(new BoolSetting("Sticky Target", true));
    private LivingEntity locked;

    public Aimbot() { super("Aimbot", "Smoothly aims at nearby targets", Category.COMBAT); }

    private boolean valid(Entity e, ClientPlayerEntity p) {
        if (!(e instanceof LivingEntity le) || e == p || !le.isAlive() || le.isSpectator()) return false;
        if (e instanceof PlayerEntity pe && dev.aether.util.Friends.is(pe.getName().getString())) return false;
        if (e instanceof PlayerEntity) return players.get();
        if (e instanceof Monster) return hostiles.get();
        if (e instanceof PassiveEntity) return passives.get();
        return false;
    }

    private Vec3d aimPoint(LivingEntity e) {
        double h = e.getHeight();
        double f = switch (aimAt.get()) { case "Body" -> 0.55; case "Legs" -> 0.2; default -> 0.0; };
        if (f == 0.0) return e.getEyePos();
        return new Vec3d(e.getX(), e.getY() + h * f, e.getZ());
    }

    @Override public void onTick(MinecraftClient mc) {
        if (mc.currentScreen != null) return;
        boolean held = switch (activate.get()) {
            case "Hold Attack" -> mc.options.attackKey.isPressed();
            case "Hold Use" -> mc.options.useKey.isPressed();
            default -> true;
        };
        if (!held) { locked = null; return; }
        ClientPlayerEntity p = mc.player;
        Vec3d eye = p.getEyePos();
        Vec3d look = p.getRotationVec(1.0f);

        LivingEntity best = null; double bestAngle = Double.MAX_VALUE; Vec3d bestPoint = null;
        for (Entity e : mc.world.getEntities()) {
            if (sticky.get() && locked != null && locked.isAlive() && e != locked) continue;
            if (!valid(e, p)) continue;
            LivingEntity le = (LivingEntity) e;
            if (p.distanceTo(le) > range.get()) continue;
            Vec3d pt = aimPoint(le);
            Vec3d dir = pt.subtract(eye);
            double len = Math.sqrt(dir.lengthSquared());
            if (len < 1e-3) continue;
            double dot = (dir.x * look.x + dir.y * look.y + dir.z * look.z) / len;
            double angle = Math.toDegrees(Math.acos(MathHelper.clamp(dot, -1.0, 1.0)));
            if (angle > fov.get() / 2.0) continue;
            if (needLos.get() && !p.canSee(le)) continue;
            if (angle < bestAngle) { bestAngle = angle; best = le; bestPoint = pt; }
        }
        if (best == null) { locked = null; return; }
        locked = best;

        double dx = bestPoint.x - eye.x, dy = bestPoint.y - eye.y, dz = bestPoint.z - eye.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        float ty = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float tp = (float) -Math.toDegrees(Math.atan2(dy, flat));

        float dyaw = MathHelper.wrapDegrees(ty - p.getYaw());
        float dpitch = tp - p.getPitch();
        float max = maxSpeed.f();
        float sm = smooth.f();
        p.setYaw(p.getYaw() + MathHelper.clamp(dyaw / sm, -max, max));
        p.setPitch(MathHelper.clamp(p.getPitch() + MathHelper.clamp(dpitch / sm, -max, max), -90f, 90f));
    }
}
