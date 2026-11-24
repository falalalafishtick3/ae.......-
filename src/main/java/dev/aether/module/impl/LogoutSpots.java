package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import java.util.*;

/** Marks where players disconnected (they vanish from the tab list while you were near them). */
public class LogoutSpots extends EspModule {
    private record Seen(String name, Box box) {}
    private record Spot(String name, Box box, long time) {}

    private final NumberSetting expire = add(new NumberSetting("Keep For (min)", 5, 1, 30, 1));
    private final BoolSetting notify = add(new BoolSetting("Chat Notify", true));
    private final ColorSetting color = add(new ColorSetting("Color", 0.0f, 0.8f, 1f));

    private final Map<UUID, Seen> seen = new HashMap<>();
    private final List<Spot> spots = new ArrayList<>();

    public LogoutSpots() {
        super("LogoutSpots", "Shows where players logged out", Category.RENDER);
        addCommonSettings();
        scanInterval.set(5.0);
        tracers.set(true);
    }

    @Override public void onDisable() { super.onDisable(); seen.clear(); spots.clear(); }

    @Override protected void scan(MinecraftClient mc, List<Target> out) {
        Set<UUID> present = new HashSet<>();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof PlayerEntity o) || o == mc.player) continue;
            present.add(o.getUuid());
            double w = o.getWidth() / 2.0;
            seen.put(o.getUuid(), new Seen(o.getName().getString(),
                    new Box(o.getX() - w, o.getY(), o.getZ() - w, o.getX() + w, o.getY() + o.getHeight(), o.getZ() + w)));
        }
        Iterator<Map.Entry<UUID, Seen>> it = seen.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            if (present.contains(en.getKey())) continue;
            // gone from the world: if also gone from the tab list, they logged out
            if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(en.getKey()) == null) {
                Seen s = en.getValue();
                spots.add(new Spot(s.name(), s.box(), System.currentTimeMillis()));
                if (notify.get()) mc.player.sendMessage(Text.literal(String.format("[Aether] %s logged out at %d, %d, %d", s.name(),
                        (int) s.box().getCenter().x, (int) s.box().minY, (int) s.box().getCenter().z)), false);
            }
            it.remove(); // also stops tracking players who merely left render distance
        }
        long now = System.currentTimeMillis(), life = expire.i() * 60_000L;
        spots.removeIf(s -> now - s.time() > life);
        for (Spot s : spots) out.add(new Target(s.box(), color.rgb()));
    }
}
