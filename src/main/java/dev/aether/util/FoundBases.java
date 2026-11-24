package dev.aether.util;

import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;

/** Saved base finds (shared by BaseHunter, BaseMap and the Hub screen). */
public final class FoundBases {
    public static class Entry { public String server = "", dim = ""; public int x, z, score, minY, maxY; public long time; }

    private static List<Entry> list;

    private static synchronized List<Entry> ensure() {
        if (list == null) list = new ArrayList<>(Store.load("bases.json", new TypeToken<List<Entry>>() {}.getType(), new ArrayList<Entry>()));
        return list;
    }
    public static synchronized List<Entry> all() { return new ArrayList<>(ensure()); }
    public static synchronized void add(Entry e) {
        for (Entry o : ensure()) if (o.server.equals(e.server) && o.dim.equals(e.dim) && o.x == e.x && o.z == e.z) return;
        ensure().add(e); Store.save("bases.json", list);
    }
    public static synchronized void remove(Entry e) { ensure().remove(e); Store.save("bases.json", list); }
    public static synchronized void clear() { ensure().clear(); Store.save("bases.json", list); }
}
