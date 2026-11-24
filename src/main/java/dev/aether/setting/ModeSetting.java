package dev.aether.setting;

import com.google.gson.*;
import java.util.List;

public class ModeSetting extends Setting<String> {
    public final List<String> modes;

    /** First entry is the default. */
    public ModeSetting(String name, String... modes) { super(name, modes[0]); this.modes = List.of(modes); }
    public boolean is(String m) { return value.equals(m); }
    public void cycle(int dir) {
        int i = (modes.indexOf(value) + dir + modes.size()) % modes.size();
        value = modes.get(i);
    }
    public JsonElement save() { return new JsonPrimitive(value); }
    public void load(JsonElement e) { if (modes.contains(e.getAsString())) value = e.getAsString(); }
}
