package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.Alerts;
import dev.aether.util.FoundBases;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.vehicle.AbstractChestBoatEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.entity.vehicle.HopperMinecartEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import java.util.*;

/** Base finder based on ENTITIES (item frames, armor stands, paintings, chest minecarts/boats, named mobs, dropped items). */
public class EntityStash extends EspModule {
    private record Hit(int cx, int cz, int score, double minY, double maxY) {}

    private final NumberSetting minScore = add(new NumberSetting("Min Score", 8, 2, 100, 1));
    private final BoolSetting frames = add(new BoolSetting("Item Frames (+ contents)", true));
    private final BoolSetting stands = add(new BoolSetting("Armor Stands", true));
    private final BoolSetting paintings = add(new BoolSetting("Paintings", true));
    private final BoolSetting vehicles = add(new BoolSetting("Chest Minecarts / Boats", true));
    private final BoolSetting named = add(new BoolSetting("Named Mobs", true));
    private final BoolSetting items = add(new BoolSetting("Dropped Items (clusters)", false));
    private final BoolSetting notify = add(new BoolSetting("Chat Notify", true));
    private final ColorSetting color = add(new ColorSetting("Color", 0.08f, 0.8f, 1f));

    private final Map<Long, Hit> shown = new HashMap<>();
    private final Set<Long> notified = new HashSet<>();

    public EntityStash() {
        super("EntityStash", "Finds bases by item frames, armor stands, minecarts...", Category.WORLD);
        addCommonSettings();
        range.set(160.0);
    }

    @Override public void onDisable() { super.onDisable(); shown.clear(); notified.clear(); }

    private int weight(Entity e) {
        if (e instanceof ItemFrameEntity f) return frames.get() ? (f.getHeldItemStack().isEmpty() ? 1 : 3) : 0;
        if (e instanceof ArmorStandEntity) return stands.get() ? 2 : 0;
        if (e instanceof PaintingEntity) return paintings.get() ? 1 : 0;
        if (e instanceof ChestMinecartEntity || e instanceof HopperMinecartEntity || e instanceof FurnaceMinecartEntity || e instanceof AbstractChestBoatEntity)
            return vehicles.get() ? 3 : 0;
        if (e instanceof ItemEntity) return items.get() ? 1 : 0;
        if (named.get() && e.hasCustomName() && !(e instanceof net.minecraft.entity.player.PlayerEntity)) return 2;
        return 0;
    }

    @Override protected void scan(MinecraftClient mc, List<Target> out) {
        Map<Long, int[]> score = new HashMap<>();
        Map<Long, double[]> ys = new HashMap<>();
        double px = mc.player.getX(), pz = mc.player.getZ(), r2 = range.get() * range.get();
        for (Entity e : mc.world.getEntities()) {
            int w = weight(e);
            if (w <= 0) continue;
            double dx = e.getX() - px, dz = e.getZ() - pz;
            if (dx * dx + dz * dz > r2) continue;
            ChunkPos cp = e.getChunkPos();
            long key = cp.toLong();
            score.computeIfAbsent(key, k -> new int[1])[0] += w;
            double[] y = ys.computeIfAbsent(key, k -> new double[]{Double.MAX_VALUE, -Double.MAX_VALUE});
            y[0] = Math.min(y[0], e.getY()); y[1] = Math.max(y[1], e.getY());
        }
        shown.clear();
        for (var en : score.entrySet()) {
            if (en.getValue()[0] < minScore.i()) continue;
            ChunkPos cp = new ChunkPos(en.getKey());
            double[] y = ys.get(en.getKey());
            shown.put(en.getKey(), new Hit(cp.x, cp.z, en.getValue()[0], y[0], y[1]));
            out.add(new Target(new Box(cp.x * 16, y[0] - 1, cp.z * 16, cp.x * 16 + 16, y[1] + 3, cp.z * 16 + 16), color.rgb()));
            if (notified.add(en.getKey())) {
                int x = cp.x * 16 + 8, z = cp.z * 16 + 8;
                if (notify.get()) mc.player.sendMessage(Text.literal("[Aether] EntityStash: activity at " + x + ", " + z + " (score " + en.getValue()[0] + ")"), false);
                FoundBases.Entry fe = new FoundBases.Entry();
                fe.server = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "singleplayer";
                fe.dim = mc.world.getRegistryKey().getValue().getPath();
                fe.x = x; fe.z = z; fe.score = en.getValue()[0]; fe.minY = (int) y[0]; fe.maxY = (int) y[1]; fe.time = System.currentTimeMillis();
                FoundBases.add(fe);
                Alerts.fire(mc, "Entity stash", x, z, en.getValue()[0]);
            }
        }
    }
}
