package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import java.util.List;

/** Marks 1x1 holes (for crystal PvP): bedrock-safe, obsidian-safe or any solid walls. */
public class HoleESP extends EspModule {
    private final NumberSetting radius = add(new NumberSetting("Radius", 8, 3, 16, 1));
    private final NumberSetting vertical = add(new NumberSetting("Vertical Range", 4, 1, 10, 1));
    private final BoolSetting anySolid = add(new BoolSetting("Include Weak Holes", false));
    private final NumberSetting maxHoles = add(new NumberSetting("Max Holes", 80, 10, 400, 10));
    private final ColorSetting cBedrock = add(new ColorSetting("Bedrock Hole", 0.38f, 0.8f, 1f));
    private final ColorSetting cObsidian = add(new ColorSetting("Obsidian Hole", 0.12f, 0.9f, 1f));
    private final ColorSetting cWeak = add(new ColorSetting("Weak Hole", 0.0f, 0.8f, 1f));

    public HoleESP() {
        super("HoleESP", "Highlights safe holes", Category.RENDER);
        cWeak.visibleWhen(anySolid::get);
        addCommonSettings();
        renderMode.set("Both");
        scanInterval.set(10.0);
        throughWalls.set(false);
    }

    private static boolean isBed(BlockState s) { return s.isOf(Blocks.BEDROCK); }
    private static boolean isObs(BlockState s) { return s.isOf(Blocks.OBSIDIAN) || s.isOf(Blocks.CRYING_OBSIDIAN) || s.isOf(Blocks.ENDER_CHEST) || s.isOf(Blocks.RESPAWN_ANCHOR); }

    @Override protected void scan(MinecraftClient mc, List<Target> out) {
        var w = mc.world;
        BlockPos c = mc.player.getBlockPos();
        int r = radius.i(), vy = vertical.i(), limit = maxHoles.i();
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) for (int dy = -vy; dy <= vy; dy++) {
            m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
            if (!w.getBlockState(m).isAir() || !w.getBlockState(m.up()).isAir() || !w.getBlockState(m.up(2)).isAir()) continue;
            BlockState below = w.getBlockState(m.down());
            if (!isBed(below) && !isObs(below) && !(anySolid.get() && below.isSolidBlock(w, m.down()))) continue;
            int bed = isBed(below) ? 1 : 0, obs = isObs(below) ? 1 : 0, weak = 0;
            boolean ok = true;
            for (Direction d : Direction.Type.HORIZONTAL) {
                BlockPos n = m.offset(d);
                BlockState s = w.getBlockState(n);
                if (isBed(s)) bed++; else if (isObs(s)) obs++; else if (anySolid.get() && s.isSolidBlock(w, n)) weak++; else { ok = false; break; }
            }
            if (!ok) continue;
            int rgb = weak > 0 ? cWeak.rgb() : (obs > 0 ? cObsidian.rgb() : cBedrock.rgb());
            out.add(new Target(new Box(m.getX(), m.getY(), m.getZ(), m.getX() + 1, m.getY() + 0.06, m.getZ() + 1), rgb));
            if (out.size() >= limit) return;
        }
    }
}
