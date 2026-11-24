package dev.aether.module.impl;

import com.google.gson.reflect.TypeToken;
import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.Store;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import java.util.*;

/** ESP for ANY blocks you choose with chat commands: .esp add <block> | remove <block> | list | clear. */
public class CustomESP extends EspModule {
    public static CustomESP INSTANCE;
    private static final List<String> IDS = new ArrayList<>(Store.load("esp_blocks.json", new TypeToken<List<String>>() {}.getType(), new ArrayList<String>()));
    private static final Map<Block, Integer> INDEX = new HashMap<>();
    private static boolean dirty = true;

    private final NumberSetting minY = add(new NumberSetting("Min Y", -64, -64, 320, 1));
    private final NumberSetting maxY = add(new NumberSetting("Max Y", 320, -64, 320, 1));
    private final NumberSetting maxBoxes = add(new NumberSetting("Max Boxes", 300, 50, 2000, 50));
    private final NumberSetting perScan = add(new NumberSetting("Chunks Per Scan", 3, 1, 16, 1));

    private static final int RESCAN_AFTER = 40;
    private final Map<Long, List<int[]>> cache = new HashMap<>();
    private final Map<Long, Integer> lastScan = new HashMap<>();
    private ClientWorld lastWorld;
    private int scanNo;

    public CustomESP() {
        super("CustomESP", "ESP for blocks you add with .esp add <block>", Category.WORLD);
        INSTANCE = this;
        addCommonSettings();
        range.set(64.0);
        renderMode.set("Both");
        fillOpacity.set(18.0);
    }

    // ---------------- list management (used by Commands) ----------------
    public static String add(String name) {
        Identifier id = Identifier.tryParse(name.contains(":") ? name : "minecraft:" + name.toLowerCase());
        if (id == null || !Registries.BLOCK.containsId(id)) return null;
        String s = id.toString();
        if (!IDS.contains(s)) IDS.add(s);
        save(); return s;
    }
    public static boolean remove(String name) {
        boolean r = IDS.remove(name.contains(":") ? name : "minecraft:" + name.toLowerCase());
        save(); return r;
    }
    public static void clear() { IDS.clear(); save(); }
    public static List<String> list() { return new ArrayList<>(IDS); }
    private static void save() { Store.save("esp_blocks.json", IDS); dirty = true; }

    private static void rebuild() {
        INDEX.clear();
        for (int i = 0; i < IDS.size(); i++) {
            Identifier id = Identifier.tryParse(IDS.get(i));
            if (id != null && Registries.BLOCK.containsId(id)) INDEX.put(Registries.BLOCK.get(id), i);
        }
        dirty = false;
    }
    private static int colorOf(int i) { return MathHelper.hsvToRgb((i * 0.618034f) % 1f, 0.8f, 1f) & 0xFFFFFF; }

    @Override public void onDisable() { super.onDisable(); cache.clear(); lastScan.clear(); }

    private List<int[]> scanChunk(WorldChunk c) {
        List<int[]> found = new ArrayList<>();
        int y0 = minY.i(), y1 = maxY.i();
        ChunkSection[] secs = c.getSectionArray();
        int bottom = c.getBottomY();
        int cx = c.getPos().getStartX(), cz = c.getPos().getStartZ();
        java.util.function.Predicate<BlockState> pred = st -> INDEX.containsKey(st.getBlock());
        for (int i = 0; i < secs.length; i++) {
            ChunkSection s = secs[i];
            if (s == null || s.isEmpty()) continue;
            int sy = bottom + 16 * i;
            if (sy + 15 < y0 || sy > y1 || !s.hasAny(pred)) continue;
            for (int y = 0; y < 16; y++) {
                int wy = sy + y;
                if (wy < y0 || wy > y1) continue;
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                    Integer t = INDEX.get(s.getBlockState(x, y, z).getBlock());
                    if (t != null) found.add(new int[]{cx + x, wy, cz + z, t});
                }
            }
        }
        return found;
    }

    @Override protected void scan(MinecraftClient mc, List<Target> out) {
        if (dirty) { rebuild(); cache.clear(); lastScan.clear(); }
        if (mc.world != lastWorld) { cache.clear(); lastScan.clear(); lastWorld = mc.world; }
        if (INDEX.isEmpty()) return;
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
        for (var e : cache.entrySet()) {
            ChunkPos cp = new ChunkPos(e.getKey());
            if (Math.abs(cp.x - pc.x) > cr || Math.abs(cp.z - pc.z) > cr) continue;
            for (int[] o : e.getValue()) {
                double dx = o[0] + 0.5 - px, dy = o[1] + 0.5 - py, dz = o[2] + 0.5 - pz;
                if (dx * dx + dy * dy + dz * dz > r2) continue;
                out.add(new Target(new Box(o[0], o[1], o[2], o[0] + 1, o[1] + 1, o[2] + 1), colorOf(o[3])));
                if (out.size() >= limit) return;
            }
        }
    }
}
