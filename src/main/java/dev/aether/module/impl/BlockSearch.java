package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import java.util.*;

/** Highlights base-revealing blocks through walls: torches, portals, beacons, TNT, rails, etc. */
public class BlockSearch extends EspModule {
    private static final class Group {
        final BoolSetting on; final ColorSetting color;
        Group(BoolSetting on, ColorSetting color) { this.on = on; this.color = color; }
    }
    private final List<Group> groups = new ArrayList<>();
    private final Map<Block, Integer> index = new HashMap<>();

    private final NumberSetting minY = add(new NumberSetting("Min Y", -64, -64, 320, 1));
    private final NumberSetting maxY = add(new NumberSetting("Max Y", 320, -64, 320, 1));
    private final NumberSetting maxBoxes = add(new NumberSetting("Max Boxes", 300, 50, 2000, 50));
    private final NumberSetting perScan = add(new NumberSetting("Chunks Per Scan", 3, 1, 16, 1));

    private static final int RESCAN_AFTER = 40;
    private final Map<Long, List<int[]>> cache = new HashMap<>();
    private final Map<Long, Integer> lastScan = new HashMap<>();
    private ClientWorld lastWorld;
    private int scanNo;

    private void group(String name, boolean def, float hue, float sat, float br, Block... blocks) {
        Group g = new Group(add(new BoolSetting(name, def)), add(new ColorSetting(name + " Color", hue, sat, br)));
        g.color.visibleWhen(g.on::get);
        for (Block b : blocks) index.put(b, groups.size());
        groups.add(g);
    }

    public BlockSearch() {
        super("BlockSearch", "Finds torches, portals, beacons, TNT, rails and more", Category.WORLD);
        group("Torches / Lanterns", true, 0.12f, 0.9f, 1f, Blocks.TORCH, Blocks.WALL_TORCH, Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH,
                Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH, Blocks.LANTERN, Blocks.SOUL_LANTERN);
        group("Portals", true, 0.78f, 0.8f, 1f, Blocks.NETHER_PORTAL, Blocks.END_PORTAL, Blocks.END_PORTAL_FRAME);
        group("Beacon", true, 0.5f, 0.5f, 1f, Blocks.BEACON);
        group("TNT", false, 0.0f, 0.9f, 1f, Blocks.TNT);
        group("Obsidian", false, 0.75f, 0.6f, 0.6f, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN);
        group("Rails", false, 0.08f, 0.5f, 0.8f, Blocks.RAIL, Blocks.POWERED_RAIL, Blocks.DETECTOR_RAIL, Blocks.ACTIVATOR_RAIL);
        group("Cobweb", false, 0.0f, 0.0f, 1f, Blocks.COBWEB);
        group("Workstations", false, 0.33f, 0.6f, 1f, Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER,
                Blocks.ENCHANTING_TABLE, Blocks.ANVIL, Blocks.BREWING_STAND);
        addCommonSettings();
        range.set(64.0);
        renderMode.set("Both");
        fillOpacity.set(18.0);
    }

    @Override public void onDisable() { super.onDisable(); cache.clear(); lastScan.clear(); }

    private List<int[]> scanChunk(WorldChunk c) {
        List<int[]> found = new ArrayList<>();
        int y0 = minY.i(), y1 = maxY.i();
        ChunkSection[] secs = c.getSectionArray();
        int bottom = c.getBottomY();
        int cx = c.getPos().getStartX(), cz = c.getPos().getStartZ();
        java.util.function.Predicate<BlockState> pred = st -> index.containsKey(st.getBlock());
        for (int i = 0; i < secs.length; i++) {
            ChunkSection s = secs[i];
            if (s == null || s.isEmpty()) continue;
            int sy = bottom + 16 * i;
            if (sy + 15 < y0 || sy > y1 || !s.hasAny(pred)) continue;
            for (int y = 0; y < 16; y++) {
                int wy = sy + y;
                if (wy < y0 || wy > y1) continue;
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                    Integer t = index.get(s.getBlockState(x, y, z).getBlock());
                    if (t != null) found.add(new int[]{cx + x, wy, cz + z, t});
                }
            }
        }
        return found;
    }

    @Override protected void scan(MinecraftClient mc, List<Target> out) {
        if (mc.world != lastWorld) { cache.clear(); lastScan.clear(); lastWorld = mc.world; }
        scanNo++;
        ChunkPos pc = mc.player.getChunkPos();
        int cr = (int) Math.ceil(range.get() / 16.0);
        int budget = perScan.i();
        long deadline = System.nanoTime() + ClickGuiModule.scanBudgetNs();
        outer:
        for (int dx = -cr; dx <= cr; dx++) for (int dz = -cr; dz <= cr; dz++) {
            long key = new ChunkPos(pc.x + dx, pc.z + dz).toLong();
            Integer last = lastScan.get(key);
            if (last != null && scanNo - last < RESCAN_AFTER) continue;
            WorldChunk c = mc.world.getChunkManager().getWorldChunk(pc.x + dx, pc.z + dz);
            if (c == null) continue;
            cache.put(key, scanChunk(c));
            lastScan.put(key, scanNo);
            if (--budget <= 0 || System.nanoTime() > deadline) break outer;
        }
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ(), r2 = range.get() * range.get();
        int limit = maxBoxes.i();
        for (Map.Entry<Long, List<int[]>> e : cache.entrySet()) {
            ChunkPos cp = new ChunkPos(e.getKey());
            if (Math.abs(cp.x - pc.x) > cr || Math.abs(cp.z - pc.z) > cr) continue;
            for (int[] o : e.getValue()) {
                Group g = groups.get(o[3]);
                if (!g.on.get()) continue;
                double dx = o[0] + 0.5 - px, dy = o[1] + 0.5 - py, dz = o[2] + 0.5 - pz;
                if (dx * dx + dy * dy + dz * dz > r2) continue;
                out.add(new Target(new Box(o[0], o[1], o[2], o[0] + 1, o[1] + 1, o[2] + 1), g.color.rgb()));
                if (out.size() >= limit) return;
            }
        }
    }
}
