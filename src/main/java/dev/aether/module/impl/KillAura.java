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
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Attacks nearby targets automatically. */
public class KillAura extends ClientModule {
    private final NumberSetting range    = add(new NumberSetting("Range", 3.2, 2.0, 6.0, 0.1));
    private final NumberSetting cooldown = add(new NumberSetting("Cooldown %", 95, 10, 100, 1));
    private final ModeSetting priority   = add(new ModeSetting("Priority", "Closest", "Lowest Health"));
    private final BoolSetting rotate     = add(new BoolSetting("Rotate To Target", true));
    private final BoolSetting players    = add(new BoolSetting("Players", true));
    private final BoolSetting hostiles   = add(new BoolSetting("Hostiles", true));
    private final BoolSetting passives   = add(new BoolSetting("Passives", false));
    private final BoolSetting needLos    = add(new BoolSetting("Require Line Of Sight", true));

    public KillAura() { super("KillAura", "Auto-attacks nearby entities", Category.COMBAT); }

    private boolean valid(Entity e, ClientPlayerEntity p) {
        if (!(e instanceof LivingEntity le) || e == p || !le.isAlive() || le.isSpectator()) return false;
        if (e instanceof PlayerEntity pe && dev.aether.util.Friends.is(pe.getName().getString())) return false;
        if (e instanceof PlayerEntity) return players.get();
        if (e instanceof Monster) return hostiles.get();
        if (e instanceof PassiveEntity) return passives.get();
        return false;
    }

    @Override public void onTick(MinecraftClient mc) {
        if (mc.currentScreen != null) return;
        var p = mc.player;
        LivingEntity best = null; double bestKey = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (!valid(e, p)) continue;
            LivingEntity le = (LivingEntity) e;
            double d = p.distanceTo(le);
            if (d > range.get()) continue;
            if (needLos.get() && !p.canSee(le)) continue;
            double key = priority.is("Lowest Health") ? le.getHealth() : d;
            if (key < bestKey) { bestKey = key; best = le; }
        }
        if (best == null) return;

        if (rotate.get()) {
            Vec3d eye = p.getEyePos(), t = best.getEyePos();
            double dx = t.x - eye.x, dy = t.y - eye.y, dz = t.z - eye.z;
            p.setYaw((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
            p.setPitch(MathHelper.clamp((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))), -90f, 90f));
        }
        if (p.getAttackCooldownProgress(0f) < cooldown.f() / 100f) return;
        mc.interactionManager.attackEntity(p, best);
        p.swingHand(Hand.MAIN_HAND);
    }
}
