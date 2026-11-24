package dev.aether.module.impl;

import com.google.gson.reflect.TypeToken;
import dev.aether.module.*;
import dev.aether.util.Store;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import java.util.ArrayList;
import java.util.List;

/** Remembers every player you see (where + when). Read it with the .seen command. */
public class PlayerLog extends ClientModule {
    public static class Seen { public String name = "", dim = "", server = ""; public int x, y, z; public long time; }
    private static List<Seen> list;
    private int tick;

    public PlayerLog() { super("PlayerLog", "Records where you saw players (.seen)", Category.WORLD); }

    private static synchronized List<Seen> ensure() {
        if (list == null) list = new ArrayList<>(Store.load("players_seen.json", new TypeToken<List<Seen>>() {}.getType(), new ArrayList<Seen>()));
        return list;
    }
    public static synchronized List<Seen> all() { return new ArrayList<>(ensure()); }

    @Override public void onDisable() { synchronized (PlayerLog.class) { if (list != null) Store.save("players_seen.json", list); } }

    @Override public void onTick(MinecraftClient mc) {
        if (++tick % 20 != 0) return;
        String dim = mc.world.getRegistryKey().getValue().getPath();
        String server = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "singleplayer";
        boolean changed = false;
        synchronized (PlayerLog.class) {
            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof PlayerEntity p) || p == mc.player) continue;
                String name = p.getName().getString();
                Seen s = null;
                for (Seen o : ensure()) if (o.name.equalsIgnoreCase(name) && o.server.equals(server)) { s = o; break; }
                if (s == null) { s = new Seen(); s.name = name; s.server = server; ensure().add(s); }
                if (System.currentTimeMillis() - s.time > 10_000 || !s.dim.equals(dim)) changed = true;
                s.dim = dim; s.x = (int) p.getX(); s.y = (int) p.getY(); s.z = (int) p.getZ(); s.time = System.currentTimeMillis();
            }
            if (changed && tick % 400 == 0) Store.save("players_seen.json", ensure());
        }
    }
}
