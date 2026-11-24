package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.WaypointStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/** Walks in a straight line to a waypoint (jumps when blocked). */
public class WaypointWalk extends ClientModule {
    private final ModeSetting target = add(new ModeSetting("Target", "Nearest", "Newest"));
    private final NumberSetting stop = add(new NumberSetting("Stop Distance", 3, 1, 20, 1));
    private final NumberSetting turn = add(new NumberSetting("Turn Speed", 18, 3, 90, 1));
    private final BoolSetting sprint = add(new BoolSetting("Sprint", true));

    public WaypointWalk() { super("WaypointWalk", "Walk to your waypoint automatically", Category.MOVEMENT); }

    private void release(MinecraftClient mc) { mc.options.forwardKey.setPressed(false); mc.options.jumpKey.setPressed(false); }
    @Override public void onDisable() { release(MinecraftClient.getInstance()); }

    @Override public void onTick(MinecraftClient mc) {
        var p = mc.player;
        String dim = mc.world.getRegistryKey().getValue().getPath();
        WaypointStore.Entry best = null; double bd = Double.MAX_VALUE;
        for (WaypointStore.Entry e : WaypointStore.all()) {
            if (!e.dim.equals(dim)) continue;
            double d = Math.hypot(e.x + 0.5 - p.getX(), e.z + 0.5 - p.getZ());
            if (target.is("Newest")) { best = e; bd = d; } else if (d < bd) { bd = d; best = e; }
        }
        if (best == null) { p.sendMessage(Text.literal("[Aether] WaypointWalk: no waypoint in this dimension"), false); setEnabled(false); return; }
        if (bd <= stop.i()) { p.sendMessage(Text.literal("[Aether] Arrived at " + best.name), false); setEnabled(false); return; }
        float want = (float) Math.toDegrees(Math.atan2(-(best.x + 0.5 - p.getX()), best.z + 0.5 - p.getZ()));
        float d = MathHelper.wrapDegrees(want - p.getYaw());
        p.setYaw(p.getYaw() + MathHelper.clamp(d, -turn.f(), turn.f()));
        mc.options.forwardKey.setPressed(true);
        mc.options.jumpKey.setPressed(p.horizontalCollision || p.isTouchingWater());
        if (sprint.get()) p.setSprinting(true);
    }
}
