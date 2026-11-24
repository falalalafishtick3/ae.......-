package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.InvUtil;
import dev.aether.util.WaypointStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

/**
 * Basic elytra autopilot: holds altitude and heading (or flies to a waypoint) and fires rockets when you get slow.
 * Start it while gliding. Disables itself near the waypoint / when you land.
 */
public class ElytraPilot extends ClientModule {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Heading", "Waypoint"));
    private final BoolSetting holdAlt = add(new BoolSetting("Hold Altitude", true));
    private final NumberSetting altitude = add(new NumberSetting("Fixed Altitude (0 = current)", 0, 0, 320, 5));
    private final BoolSetting rockets = add(new BoolSetting("Auto Firework", true));
    private final NumberSetting minSpeed = add(new NumberSetting("Rocket Below Speed (b/s)", 18, 8, 40, 1));
    private final NumberSetting pitchLimit = add(new NumberSetting("Pitch Limit", 30, 10, 60, 1));
    private final NumberSetting turn = add(new NumberSetting("Turn Speed", 4, 1, 15, 0.5));

    private float heading; private double targetY; private boolean started; private int cd;

    public ElytraPilot() { super("ElytraPilot", "Elytra autopilot (heading / waypoint, altitude hold, auto rockets)", Category.MOVEMENT); }

    @Override public void onEnable() { started = false; }

    @Override public void onTick(MinecraftClient mc) {
        var p = mc.player;
        if (!p.isGliding()) { started = false; return; }
        if (!started) { heading = p.getYaw(); targetY = altitude.i() > 0 ? altitude.i() : p.getY(); started = true; }
        if (cd > 0) cd--;

        float yawWant = heading;
        if (mode.is("Waypoint")) {
            String dim = mc.world.getRegistryKey().getValue().getPath();
            WaypointStore.Entry best = null; double bd = Double.MAX_VALUE;
            for (WaypointStore.Entry e : WaypointStore.all()) {
                if (!e.dim.equals(dim)) continue;
                double d = Math.hypot(e.x + 0.5 - p.getX(), e.z + 0.5 - p.getZ());
                if (d < bd) { bd = d; best = e; }
            }
            if (best == null) { p.sendMessage(Text.literal("[Aether] ElytraPilot: no waypoint in this dimension"), false); setEnabled(false); return; }
            if (bd < 40) { p.sendMessage(Text.literal("[Aether] ElytraPilot: arrived near " + best.name), false); setEnabled(false); return; }
            yawWant = (float) Math.toDegrees(Math.atan2(-(best.x + 0.5 - p.getX()), best.z + 0.5 - p.getZ()));
        }
        float dy = MathHelper.wrapDegrees(yawWant - p.getYaw());
        p.setYaw(p.getYaw() + MathHelper.clamp(dy, -turn.f(), turn.f()));

        if (holdAlt.get()) {
            double err = targetY - p.getY();                       // + = need to climb
            float want = MathHelper.clamp((float) (-err * 0.5), -pitchLimit.f(), 12f);
            p.setPitch(p.getPitch() + MathHelper.clamp(want - p.getPitch(), -5f, 5f));
        }

        double speed = p.getVelocity().length() * 20.0;
        if (rockets.get() && cd == 0 && speed < minSpeed.f()) {
            if (p.getOffHandStack().isOf(Items.FIREWORK_ROCKET)) { mc.interactionManager.interactItem(p, Hand.OFF_HAND); cd = 30; }
            else {
                int slot = InvUtil.find(mc, s -> s.isOf(Items.FIREWORK_ROCKET));
                if (slot >= 0) {
                    int prev = InvUtil.selected(mc);
                    InvUtil.select(mc, slot);
                    mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
                    InvUtil.select(mc, prev);
                    cd = 30;
                }
            }
        }
    }
}
