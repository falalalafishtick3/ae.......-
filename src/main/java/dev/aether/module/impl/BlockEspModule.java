package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.chunk.WorldChunk;
import java.util.List;

public abstract class BlockEspModule extends EspModule {
    protected NumberSetting inflate;

    protected BlockEspModule(String n, String d) { super(n, d, Category.RENDER); }

    @Override protected void addCommonSettings() {
        super.addCommonSettings();
        inflate = add(new NumberSetting("Box Inflate", 0.0, -0.2, 0.3, 0.01));
    }

    /** @return base RGB if this block entity should be highlighted, otherwise null. */
    protected abstract Integer classify(BlockEntity be);

    @Override protected void scan(MinecraftClient mc, List<Target> out) {
        ClientWorld w = mc.world;
        ChunkPos pc = mc.player.getChunkPos();
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        double r = range.get(), r2 = r * r;
        int cr = (int) Math.ceil(r / 16.0);

        for (int dx = -cr; dx <= cr; dx++) for (int dz = -cr; dz <= cr; dz++) {
            WorldChunk c = w.getChunkManager().getWorldChunk(pc.x + dx, pc.z + dz);
            if (c == null) continue;
            for (BlockEntity be : c.getBlockEntities().values()) {
                if (be.isRemoved()) continue;
                BlockPos p = be.getPos();
                if (p.getSquaredDistance(px, py, pz) > r2) continue;
                Integer rgb = classify(be);
                if (rgb == null) continue;
                VoxelShape s = be.getCachedState().getOutlineShape(w, p);
                Box b = (s.isEmpty() ? new Box(p) : s.getBoundingBox().offset(p)).expand(inflate.get());
                out.add(new Target(b, rgb));
            }
        }
    }
}
