package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Heuristic new-chunk detector (fluid-flow method): a freshly generated chunk's liquids start flowing AFTER you receive it,
 * while an already-explored chunk arrives with its liquids already flowed. Hooks: ClientPlayNetworkHandlerMixin.
 * Marks NEW chunks (someone hasn't been there) vs OLD ones. Not perfect, but good for finding where players have / haven't been.
 */
public class NewChunks extends EspModule {
    public static NewChunks INSTANCE;
    private static ClientWorld worldRef;
    private static final Set<Long> NEW = new HashSet<>(), OLD = new HashSet<>();

    private final BoolSetting showNew = add(new BoolSetting("Show New Chunks", true));
    private final BoolSetting showOld = add(new BoolSetting("Show Old Chunks", true));
    private final ModeSetting height = add(new ModeSetting("Height", "Player Y", "Fixed Y"));
    private final NumberSetting fixedY = add(new NumberSetting("Fixed Y", 64, -64, 320, 1));
    private final ColorSetting cNew = add(new ColorSetting("New Color", 0.06f, 0.85f, 1f));
    private final ColorSetting cOld = add(new ColorSetting("Old Color", 0.62f, 0.7f, 0.8f));

    public NewChunks() {
        super("NewChunks", "Marks new vs already-explored chunks", Category.WORLD);
        INSTANCE = this;
        fixedY.visibleWhen(() -> height.is("Fixed Y"));
        addCommonSettings();
        renderMode.set("Fill");
        fillOpacity.set(22.0);
        range.set(256.0);
        throughWalls.set(false);
        scanInterval.set(10.0);
    }

    @Override public void onDisable() { super.onDisable(); synchronized (NEW) { NEW.clear(); OLD.clear(); } }

    private static void checkWorld(MinecraftClient mc) {
        if (mc.world != worldRef) { synchronized (NEW) { NEW.clear(); OLD.clear(); } worldRef = mc.world; }
    }

    private static boolean flowing(BlockState s) {
        FluidState f = s.getFluidState();
        return !f.isEmpty() && !f.isStill();
    }

    /** Chunk packet arrived (main thread): already-flowed liquids => OLD chunk. */
    public static void onChunk(MinecraftClient mc, int cx, int cz) {
        checkWorld(mc);
        WorldChunk c = mc.world.getChunkManager().getWorldChunk(cx, cz);
        if (c == null) return;
        boolean old = false;
        for (ChunkSection s : c.getSectionArray()) if (s != null && !s.isEmpty() && s.hasAny(NewChunks::flowing)) { old = true; break; }
        long key = new ChunkPos(cx, cz).toLong();
        synchronized (NEW) { if (old) { OLD.add(key); NEW.remove(key); } }
    }

    /** A block update with flowing fluid arrived: if the chunk isn't known-old, it is NEW. */
    public static void onFluidUpdate(MinecraftClient mc, int bx, int bz, BlockState state) {
        if (!flowing(state)) return;
        checkWorld(mc);
        long key = new ChunkPos(bx >> 4, bz >> 4).toLong();
        synchronized (NEW) { if (!OLD.contains(key)) NEW.add(key); }
    }

    @Override protected void scan(MinecraftClient mc, List<Target> out) {
        checkWorld(mc);
        ChunkPos pc = mc.player.getChunkPos();
        int cr = (int) Math.ceil(range.get() / 16.0);
        double y = height.is("Fixed Y") ? fixedY.i() : Math.floor(mc.player.getY());
        synchronized (NEW) {
            if (showNew.get()) for (long k : NEW) add(out, k, pc, cr, y, cNew.rgb());
            if (showOld.get()) for (long k : OLD) add(out, k, pc, cr, y, cOld.rgb());
        }
    }

    private static void add(List<Target> out, long key, ChunkPos pc, int cr, double y, int rgb) {
        ChunkPos cp = new ChunkPos(key);
        if (Math.abs(cp.x - pc.x) > cr || Math.abs(cp.z - pc.z) > cr) return;
        out.add(new Target(new Box(cp.x * 16, y, cp.z * 16, cp.x * 16 + 16, y + 0.1, cp.z * 16 + 16), rgb));
    }
}
