package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.Alerts;
import dev.aether.util.FoundBases;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import java.util.*;

/**
 * Finds chunks full of blocks that don't generate naturally (metal blocks, glass, concrete/wool, redstone parts, planks, ...).
 * Catches bases that have no chests at all.
 */
public class PlacedBlocks extends EspModule {
    private static final String[] NAMES = {"Metal Blocks", "Redstone Parts", "Glass", "Wool / Concrete", "Wood Building", "Polished / Smooth", "Cobblestone", "Obsidian"};
    private static final int[] WEIGHT = {3, 2, 1, 1, 1, 1, 1, 1};
    private static final int G = NAMES.length;
    private static final Set<String> METAL = Set.of("iron_block", "gold_block", "diamond_block", "emerald_block", "netherite_block", "lapis_block",
            "redstone_block", "coal_block", "copper_block", "raw_iron_block", "raw_gold_block", "raw_copper_block");
    private static final Set<String> REDSTONE = Set.of("observer", "piston", "sticky_piston", "repeater", "comparator", "redstone_lamp",
            "slime_block", "honey_block", "target", "daylight_detector", "note_block");

    private final BoolSetting[] on = new BoolSetting[G];
    private final NumberSetting minScore = add(new NumberSetting("Min Score", 20, 5, 500, 5));
    private final NumberSetting minY = add(new NumberSetting("Min Y", -64, -64, 320, 1));
    private final NumberSetting maxY = add(new NumberSetting("Max Y", 320, -64, 320, 1));
    private final NumberSetting perScan = add(new NumberSetting("Chunks Per Scan", 3, 1, 16, 1));
    private final BoolSetting notify = add(new BoolSetting("Chat Notify", true));
    private final ColorSetting color = add(new ColorSetting("Color", 0.95f, 0.7f, 1f));

    private static final int RESCAN_AFTER = 100;
    private final Map<Block, Integer> cls = new HashMap<>();
    private final Map<Long, int[]> cache = new HashMap<>();      // [G counts..., minY, maxY]
    private final Map<Long, Integer> lastScan = new HashMap<>();
    private final Set<Long> notified = new HashSet<>();
    private ClientWorld lastWorld;
    private int scanNo;

    public PlacedBlocks() {
        super("PlacedBlocks", "Finds chunks full of non-natural blocks", Category.WORLD);
        for (int i = 0; i < G; i++) on[i] = add(new BoolSetting(NAMES[i], i <= 4));
        addCommonSettings();
        range.set(96.0);
    }

    @Override public void onDisable() { super.onDisable(); cache.clear(); lastScan.clear(); notified.clear(); }

    private int classify(Block b) {
        return cls.computeIfAbsent(b, k -> {
            String p = Registries.BLOCK.getId(k).getPath();
            if (METAL.contains(p)) return 0;
            if (REDSTONE.contains(p)) return 1;
            if (p.contains("glass")) return 2;
            if (p.endsWith("_wool") || p.endsWith("_concrete") || p.endsWith("_concrete_powder") || p.endsWith("_glazed_terracotta")) return 3;
            if (p.endsWith("_planks") || p.endsWith("_door") || p.endsWith("_trapdoor") || p.endsWith("_fence_gate") || p.endsWith("_fence")) return 4;
            if (p.startsWith("polished_") || p.startsWith("smooth_")) return 5;
            if (p.equals("cobblestone") || p.equals("mossy_cobblestone")) return 6;
            if (p.equals("obsidian") || p.equals("crying_obsidian")) return 7;
            return -1;
        });
    }

    private int[] scanChunk(WorldChunk c) {
        int[] r = new int[G + 2];
        r[G] = Integer.MAX_VALUE; r[G + 1] = Integer.MIN_VALUE;
        int y0 = minY.i(), y1 = maxY.i();
        ChunkSection[] secs = c.getSectionArray();
        int bottom = c.getBottomY();
        java.util.function.Predicate<BlockState> pred = st -> classify(st.getBlock()) >= 0;
        for (int i = 0; i < secs.length; i++) {
            ChunkSection s = secs[i];
            if (s == null || s.isEmpty()) continue;
            int sy = bottom + 16 * i;
            if (sy + 15 < y0 || sy > y1 || !s.hasAny(pred)) continue;
            for (int y = 0; y < 16; y++) {
                int wy = sy + y;
                if (wy < y0 || wy > y1) continue;
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                    int g = classify(s.getBlockState(x, y, z).getBlock());
                    if (g < 0) continue;
                    r[g]++;
                    if (wy < r[G]) r[G] = wy;
                    if (wy > r[G + 1]) r[G + 1] = wy;
                }
            }
        }
        return r;
    }

    private int score(int[] r) {
        int s = 0;
        for (int g = 0; g < G; g++) if (on[g].get()) s += Math.min(r[g], 80) * WEIGHT[g];
        return s;
    }

    @Override protected void scan(MinecraftClient mc, List<Target> out) {
        if (mc.world != lastWorld) { cache.clear(); lastScan.clear(); notified.clear(); lastWorld = mc.world; }
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
        for (var e : cache.entrySet()) {
            ChunkPos cp = new ChunkPos(e.getKey());
            if (Math.abs(cp.x - pc.x) > cr || Math.abs(cp.z - pc.z) > cr) continue;
            int[] r = e.getValue();
            int sc = score(r);
            if (sc < minScore.i() || r[G] == Integer.MAX_VALUE) continue;
            out.add(new Target(new Box(cp.x * 16, r[G] - 1, cp.z * 16, cp.x * 16 + 16, r[G + 1] + 2, cp.z * 16 + 16), color.rgb()));
            if (notified.add(e.getKey())) {
                int x = cp.x * 16 + 8, z = cp.z * 16 + 8;
                if (notify.get()) mc.player.sendMessage(Text.literal("[Aether] PlacedBlocks: suspicious chunk at " + x + ", " + z + " (score " + sc + ")"), false);
                FoundBases.Entry fe = new FoundBases.Entry();
                fe.server = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "singleplayer";
                fe.dim = mc.world.getRegistryKey().getValue().getPath();
                fe.x = x; fe.z = z; fe.score = sc; fe.minY = r[G]; fe.maxY = r[G + 1]; fe.time = System.currentTimeMillis();
                FoundBases.add(fe);
                Alerts.fire(mc, "Placed blocks", x, z, sc);
            }
        }
    }
}
