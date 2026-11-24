package dev.aether.module.impl;

import dev.aether.hud.HudUtil;
import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

/** Counts storage blocks in loaded chunks around you (chests, barrels, shulkers, hoppers, furnaces...). */
public class StorageCount extends ClientModule {
    private final ModeSetting corner = add(new ModeSetting("Position", "Bottom Left", "Top Left", "Top Right", "Bottom Right"));
    private final NumberSetting offX = add(new NumberSetting("Offset X", 6, 0, 400, 1));
    private final NumberSetting offY = add(new NumberSetting("Offset Y", 130, 0, 400, 1));
    private final NumberSetting radius = add(new NumberSetting("Chunk Radius", 8, 2, 24, 1));

    private int chests, barrels, shulkers, ender, hoppers, furnaces, spawners, tick;

    public StorageCount() { super("StorageCount", "How much storage is around you", Category.HUD); }

    @Override public void onTick(MinecraftClient mc) {
        if (++tick % 40 != 1) return;
        int c = 0, b = 0, s = 0, e = 0, h = 0, f = 0, sp = 0;
        ChunkPos pc = mc.player.getChunkPos();
        int r = radius.i();
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            WorldChunk ch = mc.world.getChunkManager().getWorldChunk(pc.x + dx, pc.z + dz);
            if (ch == null) continue;
            for (BlockEntity be : ch.getBlockEntities().values()) {
                if (be instanceof ChestBlockEntity) c++;
                else if (be instanceof BarrelBlockEntity) b++;
                else if (be instanceof ShulkerBoxBlockEntity) s++;
                else if (be instanceof EnderChestBlockEntity) e++;
                else if (be instanceof HopperBlockEntity) h++;
                else if (be instanceof AbstractFurnaceBlockEntity) f++;
                else if (be instanceof MobSpawnerBlockEntity) sp++;
            }
        }
        chests = c; barrels = b; shulkers = s; ender = e; hoppers = h; furnaces = f; spawners = sp;
    }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        var tr = MinecraftClient.getInstance().textRenderer;
        String[] lines = {"Chests: " + chests, "Barrels: " + barrels, "Shulkers: " + shulkers, "Ender chests: " + ender,
                "Hoppers: " + hoppers, "Furnaces: " + furnaces, "Spawners: " + spawners};
        int w = 0; for (String l : lines) w = Math.max(w, tr.getWidth(l));
        int h = lines.length * 10 + 4;
        int[] p = HudUtil.pos(corner.get(), offX.i(), offY.i(), w + 10, h, g.getScaledWindowWidth(), g.getScaledWindowHeight());
        HudUtil.card(g, p[0], p[1], w + 10, h, ClickGuiModule.accent());
        int y = p[1] + 3;
        for (String l : lines) { g.drawText(tr, l, p[0] + 6, y, 0xFFFFFFFF, true); y += 10; }
    }
}
