package dev.aether.util;

import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;

public final class WaypointStore {
    public static class Entry { public String name = "", dim = ""; public int x, y, z; public float hue; }

    private static List<Entry> list;

    private static synchronized List<Entry> ensure() {
        if (list == null) list = new ArrayList<>(Store.load("waypoints.json", new TypeToken<List<Entry>>() {}.getType(), new ArrayList<Entry>()));
        return list;
    }
    public static synchronized List<Entry> all() { return new ArrayList<>(ensure()); }
    public static synchronized void add(Entry e) { ensure().add(e); Store.save("waypoints.json", list); }
    public static synchronized void remove(Entry e) { ensure().remove(e); Store.save("waypoints.json", list); }
    public static synchronized void clear() { ensure().clear(); Store.save("waypoints.json", list); }
}
