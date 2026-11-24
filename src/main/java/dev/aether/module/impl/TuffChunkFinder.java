package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import java.util.*;
import java.util.function.Predicate;

/** Highlights chunks that contain a lot of tuff (natural blobs are small; big counts or variants hint at builds/structures). */
public class TuffChunkFinder extends EspModule {
    private final NumberSetting minBlocks = add(new NumberSetting("Min Tuff Blocks", 150, 1, 4096, 1));
    private final BoolSetting variants    = add(new BoolSetting("Count Tuff Variants", true));
    private final NumberSetting minY      = add(new NumberSetting("Min Y", -64, -64, 320, 1));
    private final NumberSetting maxY      = add(new NumberSetting("Max Y", 16, -64, 320, 1));
    private final ModeSetting marker      = add(new ModeSetting("Marker", "Column", "Flat"));
    private final NumberSetting perScan   = add(new NumberSetting("Chunks Per Scan", 4, 1, 32, 1));
    private final BoolSetting notify      = add(new BoolSetting("Chat Notify", true));
    private final ColorSetting color      = add(new ColorSetting("Color", 0.08f, 0.5f, 0.9f));

    private static final int RESCAN_AFTER = 60;
    private final Map<Long, Integer> counts = new HashMap<>();
    private final Map<Long, Integer> lastScan = new HashMap<>();
    private final Set<Long> notified = new HashSet<>();
    private ClientWorld lastWorld;
    private int scanNo;

    public TuffChunkFinder() {
        super("TuffChunkFinder", "Finds chunks with lots of tuff", Category.WORLD);
        addCommonSettings();
    }

    @Override public void onDisable() { super.onDisable(); counts.clear(); lastScan.clear(); notified.clear(); }

    private boolean isTuff(BlockState s) {
        if (s.isOf(Blocks.TUFF)) return true;
        return variants.get() && (s.isOf(Blocks.TUFF_BRICKS) || s.isOf(Blocks.POLISHED_TUFF)
                || s.isOf(Blocks.CHISELED_TUFF) || s.isOf(Blocks.CHISELED_TUFF_BRICKS));
    }

    private int count(WorldChunk c) {
        int y0 = minY.i(), y1 = maxY.i(), total = 0;
        ChunkSection[] secs = c.getSectionArray();
        int bottom = c.getBottomY();
        Predicate<BlockState> pred = this::isTuff;
        for (int i = 0; i < secs.length; i++) {
            ChunkSection s = secs[i];
            if (s == null || s.isEmpty()) continue;
            int sy = bottom + 16 * i;
            if (sy + 15 < y0 || sy > y1) continue;
            if (!s.hasAny(pred)) continue;
            for (int y = 0; y < 16; y++) {
                int wy = sy + y;
                if (wy < y0 || wy > y1) continue;
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++)
                    if (isTuff(s.getBlockState(x, y, z))) total++;
            }
        }
        return total;
    }

    @Override protected void scan(MinecraftClient mc, List<Target> out) {
        if (mc.world != lastWorld) { counts.clear(); lastScan.clear(); notified.clear(); lastWorld = mc.world; }
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
            int n = count(c);
            counts.put(key, n);
            lastScan.put(key, scanNo);
            if (n >= minBlocks.i() && notified.add(key)) {
                if (notify.get()) mc.player.sendMessage(Text.literal("[Aether] Tuff chunk at " + ((pc.x + dx) * 16) + ", " + ((pc.z + dz) * 16)
                        + " (" + n + " blocks)"), false);
                dev.aether.util.Alerts.fire(mc, "Tuff chunk", (pc.x + dx) * 16 + 8, (pc.z + dz) * 16 + 8, 0);
            }
            if (--budget <= 0 || System.nanoTime() > deadline) break outer;
        }

        double py = mc.player.getY();
        for (Map.Entry<Long, Integer> e : counts.entrySet()) {
            if (e.getValue() < minBlocks.i()) continue;
            ChunkPos cp = new ChunkPos(e.getKey());
            if (Math.abs(cp.x - pc.x) > cr || Math.abs(cp.z - pc.z) > cr) continue;
            double x0 = cp.x * 16, z0 = cp.z * 16;
            Box b = marker.is("Flat")
                    ? new Box(x0, py, z0, x0 + 16, py + 0.1, z0 + 16)
                    : new Box(x0, minY.i(), z0, x0 + 16, maxY.i() + 1, z0 + 16);
            out.add(new Target(b, color.rgb()));
        }
    }
}
