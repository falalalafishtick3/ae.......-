package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.WorldChunk;
import java.util.*;

/** Scores chunks by player-made block entities (storage, furnaces, beds, ...) and highlights the busy ones. */
public class BaseFinder extends EspModule {
    private final NumberSetting minScore  = add(new NumberSetting("Min Score", 8, 1, 100, 1));
    private final NumberSetting minTypes  = add(new NumberSetting("Min Distinct Types", 2, 1, 8, 1));
    private final NumberSetting minY      = add(new NumberSetting("Min Y", -64, -64, 320, 1));
    private final NumberSetting maxY      = add(new NumberSetting("Max Y", 320, -64, 320, 1));
    private final BoolSetting ignoreNatural = add(new BoolSetting("Ignore Spawner/Vault Chunks", true));
    private final BoolSetting heat        = add(new BoolSetting("Heat Color (by score)", true));
    private final BoolSetting notify      = add(new BoolSetting("Chat Notify", true));
    private final ColorSetting color      = add(new ColorSetting("Color", 0.12f, 0.85f, 1f));

    private final Set<Long> notified = new HashSet<>();

    public BaseFinder() {
        super("BaseFinder", "Highlights chunks full of player-made blocks", Category.WORLD);
        color.visibleWhen(() -> !heat.get());
        addCommonSettings();
    }

    @Override public void onDisable() { super.onDisable(); notified.clear(); }

    private static int weight(BlockEntity be) {
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

    @Override protected void scan(MinecraftClient mc, List<Target> out) {
        ChunkPos pc = mc.player.getChunkPos();
        int cr = (int) Math.ceil(range.get() / 16.0);
        int y0 = minY.i(), y1 = maxY.i();

        for (int dx = -cr; dx <= cr; dx++) for (int dz = -cr; dz <= cr; dz++) {
            int cx = pc.x + dx, cz = pc.z + dz;
            WorldChunk c = mc.world.getChunkManager().getWorldChunk(cx, cz);
            if (c == null) continue;

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
            if (score < minScore.i() || types.size() < minTypes.i()) continue;
            if (natural && ignoreNatural.get()) continue;

            int rgb = color.rgb();
            if (heat.get()) {
                float t = Math.min(1f, (score - minScore.i()) / (float) (minScore.i() * 2));
                rgb = MathHelper.hsvToRgb(0.16f * (1f - t), 0.9f, 1f);
            }
            out.add(new Target(new Box(cx * 16, lo - 1, cz * 16, cx * 16 + 16, hi + 2, cz * 16 + 16), rgb));

            if (notified.add(new ChunkPos(cx, cz).toLong())) {
                if (notify.get()) mc.player.sendMessage(Text.literal("[Aether] Possible base at " + (cx * 16) + ", " + (cz * 16)
                        + " (score " + score + ")"), false);
                dev.aether.util.Alerts.fire(mc, "Base found", cx * 16 + 8, cz * 16 + 8, score);
            }
        }
    }
}
