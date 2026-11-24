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

/** X-ray style ore highlighter (draws boxes through walls; does not change terrain rendering). */
public class OreFinder extends EspModule {
    private static final class Ore {
        final Block[] blocks; final BoolSetting on; final ColorSetting color;
        Ore(Block[] b, BoolSetting on, ColorSetting c) { blocks = b; this.on = on; color = c; }
    }

    private final List<Ore> ores = new ArrayList<>();
    private final Map<Block, Integer> index = new HashMap<>();

    private final NumberSetting minY = add(new NumberSetting("Min Y", -64, -64, 320, 1));
    private final NumberSetting maxY = add(new NumberSetting("Max Y", 320, -64, 320, 1));
    private final NumberSetting maxBoxes = add(new NumberSetting("Max Boxes", 400, 50, 2000, 50));
    private final NumberSetting perScan = add(new NumberSetting("Chunks Per Scan", 3, 1, 16, 1));

    private static final int RESCAN_AFTER = 40;
    private final Map<Long, List<int[]>> cache = new HashMap<>();
    private final Map<Long, Integer> lastScan = new HashMap<>();
    private ClientWorld lastWorld;
    private int scanNo;

    private void ore(String name, boolean def, float hue, float sat, float br, Block... blocks) {
        Ore o = new Ore(blocks, add(new BoolSetting(name, def)), add(new ColorSetting(name + " Color", hue, sat, br)));
        o.color.visibleWhen(o.on::get);
        for (Block b : blocks) index.put(b, ores.size());
        ores.add(o);
    }

    public OreFinder() {
        super("OreFinder", "Highlights ores through walls", Category.WORLD);
        ore("Diamond", true, 0.50f, 0.8f, 1f, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        ore("Emerald", true, 0.38f, 0.8f, 1f, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        ore("Ancient Debris", true, 0.06f, 0.6f, 0.7f, Blocks.ANCIENT_DEBRIS);
        ore("Gold", true, 0.14f, 0.9f, 1f, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
        ore("Iron", false, 0.08f, 0.3f, 0.9f, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        ore("Copper", false, 0.05f, 0.8f, 0.9f, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
        ore("Redstone", false, 0.0f, 0.9f, 1f, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        ore("Lapis", false, 0.62f, 0.8f, 1f, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        ore("Coal", false, 0.0f, 0.0f, 0.4f, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
        ore("Quartz", false, 0.0f, 0.0f, 1f, Blocks.NETHER_QUARTZ_ORE);
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
                Ore ore = ores.get(o[3]);
                if (!ore.on.get()) continue;
                double dx = o[0] + 0.5 - px, dy = o[1] + 0.5 - py, dz = o[2] + 0.5 - pz;
                if (dx * dx + dy * dy + dz * dz > r2) continue;
                out.add(new Target(new Box(o[0], o[1], o[2], o[0] + 1, o[1] + 1, o[2] + 1), ore.color.rgb()));
                if (out.size() >= limit) return;
            }
        }
    }
}
