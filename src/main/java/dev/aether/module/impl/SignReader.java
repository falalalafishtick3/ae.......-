package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.Store;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/** Reads nearby signs and prints their text with coordinates (signs often name base owners). */
public class SignReader extends ClientModule {
    private final NumberSetting radius = add(new NumberSetting("Chunk Radius", 6, 1, 16, 1));
    private final NumberSetting maxMsgs = add(new NumberSetting("Max Messages Per Scan", 3, 1, 10, 1));
    private final BoolSetting save = add(new BoolSetting("Save To signs.txt", true));
    private final Set<Long> seen = new HashSet<>();
    private int tick;

    public SignReader() { super("SignReader", "Prints text from nearby signs", Category.WORLD); }

    @Override public void onDisable() { seen.clear(); }

    private static String text(SignBlockEntity s, boolean front) {
        var t = front ? s.getFrontText() : s.getBackText();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            String l = t.getMessage(i, false).getString().trim();
            if (l.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" | ");
            sb.append(l);
        }
        return sb.toString();
    }

    @Override public void onTick(MinecraftClient mc) {
        if (++tick % 40 != 0) return;
        ChunkPos pc = mc.player.getChunkPos();
        int r = radius.i(), sent = 0;
        for (int dx = -r; dx <= r && sent < maxMsgs.i(); dx++) for (int dz = -r; dz <= r && sent < maxMsgs.i(); dz++) {
            WorldChunk c = mc.world.getChunkManager().getWorldChunk(pc.x + dx, pc.z + dz);
            if (c == null) continue;
            for (BlockEntity be : c.getBlockEntities().values()) {
                if (!(be instanceof SignBlockEntity sign) || sent >= maxMsgs.i()) continue;
                BlockPos p = sign.getPos();
                if (seen.contains(p.asLong())) continue;
                String front = text(sign, true), back = text(sign, false);
                String txt = front + (back.isEmpty() ? "" : "  //  " + back);
                if (txt.isEmpty()) continue;
                seen.add(p.asLong());
                sent++;
                mc.player.sendMessage(Text.literal("[Aether] Sign " + p.getX() + ", " + p.getY() + ", " + p.getZ() + ": " + txt), false);
                if (save.get()) {
                    try {
                        String server = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "singleplayer";
                        Files.writeString(Store.dir().resolve("signs.txt"),
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " | " + server + " | "
                                        + mc.world.getRegistryKey().getValue().getPath() + " | " + p.getX() + " " + p.getY() + " " + p.getZ() + " | " + txt + System.lineSeparator(),
                                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}
