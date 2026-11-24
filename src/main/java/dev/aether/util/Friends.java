package dev.aether.util;

import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Friend names (case-insensitive). Aimbot, KillAura and Triggerbot skip them; Visuals shows them in their own color. */
public final class Friends {
    private static Set<String> set;

    private static synchronized Set<String> ensure() {
        if (set == null) {
            List<String> l = Store.load("friends.json", new TypeToken<List<String>>() {}.getType(), new ArrayList<String>());
            set = new HashSet<>();
            for (String s : l) set.add(s.toLowerCase());
        }
        return set;
    }
    public static synchronized List<String> all() { List<String> l = new ArrayList<>(ensure()); java.util.Collections.sort(l); return l; }
    public static boolean is(String name) { return ensure().contains(name.toLowerCase()); }
    /** @return true if now a friend, false if removed. */
    public static synchronized boolean toggle(String name) {
        String k = name.toLowerCase();
        boolean added = ensure().add(k);
        if (!added) ensure().remove(k);
        Store.save("friends.json", new ArrayList<>(set));
        return added;
    }
}
