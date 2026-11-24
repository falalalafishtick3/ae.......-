package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.LightType;
import java.util.List;

/** Marks floor tiles where hostile mobs can spawn (block light 0). Great for lighting caves and mob-proofing bases. */
public class LightOverlay extends EspModule {
    private final NumberSetting radius = add(new NumberSetting("Radius", 8, 3, 16, 1));
    private final NumberSetting vertical = add(new NumberSetting("Vertical Range", 4, 1, 8, 1));
    private final BoolSetting ignoreSun = add(new BoolSetting("Ignore Sunlit Tiles", true));
    private final NumberSetting maxTiles = add(new NumberSetting("Max Tiles", 500, 50, 2000, 50));
    private final ColorSetting color = add(new ColorSetting("Color", 0.0f, 0.85f, 1f));

    public LightOverlay() {
        super("LightOverlay", "Shows where mobs can spawn", Category.RENDER);
        addCommonSettings();
        renderMode.set("Fill");
        fillOpacity.set(30.0);
        scanInterval.set(10.0);
        throughWalls.set(false);
    }

    @Override protected void scan(MinecraftClient mc, List<Target> out) {
        var w = mc.world;
        BlockPos c = mc.player.getBlockPos();
        int r = radius.i(), vy = vertical.i(), limit = maxTiles.i();
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) for (int dy = -vy; dy <= vy; dy++) {
            m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
            if (!w.getBlockState(m).isAir()) continue;
            BlockPos below = m.down();
            if (!w.getBlockState(below).isSolidBlock(w, below)) continue;
            if (w.getLightLevel(LightType.BLOCK, m) != 0) continue;
            if (ignoreSun.get() && w.getLightLevel(LightType.SKY, m) >= 8) continue;
            out.add(new Target(new Box(m.getX(), m.getY() + 0.02, m.getZ(), m.getX() + 1, m.getY() + 0.06, m.getZ() + 1), color.rgb()));
            if (out.size() >= limit) return;
        }
    }
}
