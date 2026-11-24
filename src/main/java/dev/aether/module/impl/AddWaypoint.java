package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.util.WaypointStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.util.concurrent.ThreadLocalRandom;

/** Action: bind a key, press it to save a waypoint at your position. */
public class AddWaypoint extends ClientModule {
    public AddWaypoint() { super("AddWaypoint", "Press its key to save a waypoint here", Category.WORLD); }

    @Override public void onEnable() {
        enabled = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        WaypointStore.Entry e = new WaypointStore.Entry();
        e.name = "WP " + (WaypointStore.all().size() + 1);
        e.dim = mc.world.getRegistryKey().getValue().getPath();
        e.x = (int) Math.floor(mc.player.getX()); e.y = (int) Math.floor(mc.player.getY()); e.z = (int) Math.floor(mc.player.getZ());
        e.hue = ThreadLocalRandom.current().nextFloat();
        WaypointStore.add(e);
        mc.player.sendMessage(Text.literal("[Aether] Saved " + e.name + " at " + e.x + ", " + e.y + ", " + e.z), false);
    }
}
