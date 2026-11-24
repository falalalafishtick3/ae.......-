package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import java.util.List;

public class ChunkBorders extends EspModule {
    private final NumberSetting radius = add(new NumberSetting("Chunk Radius", 1, 0, 4, 1));
    private final ModeSetting height = add(new ModeSetting("Height", "Around Player", "Full World"));
    private final NumberSetting around = add(new NumberSetting("Height Around Player", 24, 8, 128, 1));
    private final ColorSetting color = add(new ColorSetting("Color", 0.55f, 0.6f, 1f));

    public ChunkBorders() {
        super("ChunkBorders", "Shows chunk boundaries", Category.WORLD);
        around.visibleWhen(() -> height.is("Around Player"));
        addCommonSettings();
        // sensible defaults for a border overlay
        renderMode.set("Outline");
        throughWalls.set(false);
        scanInterval.set(5.0);
    }

    @Override protected void scan(MinecraftClient mc, List<Target> out) {
        ChunkPos pc = mc.player.getChunkPos();
        int r = radius.i();
        double y0, y1;
        if (height.is("Full World")) { y0 = mc.world.getBottomY(); y1 = mc.world.getBottomY() + mc.world.getHeight(); }
        else { y0 = Math.floor(mc.player.getY()) - around.i(); y1 = Math.floor(mc.player.getY()) + around.i(); }
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            double x0 = (pc.x + dx) * 16, z0 = (pc.z + dz) * 16;
            out.add(new Target(new Box(x0, y0, z0, x0 + 16, y1, z0 + 16), color.rgb()));
        }
    }
}
