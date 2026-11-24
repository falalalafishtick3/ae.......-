package dev.aether.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.*;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/** Scores a chunk by player-made block entities. Chunks with natural structures are flagged. */
public final class BaseScan {
    public record Result(int score, int types, boolean natural, int minY, int maxY) {}

    public static int weight(BlockEntity be) {
        if (be instanceof BeaconBlockEntity) return 6;
        if (be instanceof ShulkerBoxBlockEntity) return 3;
        if (be instanceof BrewingStandBlockEntity || be instanceof EnchantingTableBlockEntity) return 3;
        if (be instanceof EnderChestBlockEntity) return 2;
        if (be instanceof HopperBlockEntity || be instanceof DispenserBlockEntity || be instanceof CrafterBlockEntity) return 2;
        if (be instanceof BedBlockEntity) return 2;
        if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) return 1;
        if (be instanceof AbstractFurnaceBlockEntity) return 1;
        if (be instanceof JukeboxBlockEntity || be instanceof LecternBlockEntity || be instanceof ChiseledBookshelfBlockEntity) return 1;
        if (be instanceof SignBlockEntity || be instanceof BannerBlockEntity) return 1;
        return 0;
    }

    public static final Predicate<BlockState> END_STRUCTURE = st -> st.isOf(Blocks.PURPUR_BLOCK) || st.isOf(Blocks.PURPUR_PILLAR);
    public static final Predicate<BlockState> NETHER_STRUCTURE = st -> st.isOf(Blocks.NETHER_BRICKS) || st.isOf(Blocks.POLISHED_BLACKSTONE_BRICKS) || st.isOf(Blocks.GILDED_BLACKSTONE);

    private static boolean hasMarker(WorldChunk c, Predicate<BlockState> marker) {
        for (ChunkSection s : c.getSectionArray())
            if (s != null && !s.isEmpty() && s.hasAny(marker)) return true;
        return false;
    }

    public static Result scan(WorldChunk c, int y0, int y1, Predicate<BlockState> naturalMarker, int minScoreForMarkerCheck) {
        int score = 0, lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        boolean natural = false;
        Set<Class<?>> types = new HashSet<>();
        for (BlockEntity be : c.getBlockEntities().values()) {
            if (be.isRemoved()) continue;
            if (be instanceof MobSpawnerBlockEntity || be instanceof TrialSpawnerBlockEntity || be instanceof VaultBlockEntity) natural = true;
            int y = be.getPos().getY();
            if (y < y0 || y > y1) continue;
            int w = weight(be);
            if (w <= 0) continue;
            score += w; types.add(be.getClass());
            lo = Math.min(lo, y); hi = Math.max(hi, y);
        }
        if (naturalMarker != null && !natural && score >= minScoreForMarkerCheck && hasMarker(c, naturalMarker)) natural = true;
        return new Result(score, types.size(), natural, lo, hi);
    }
}
