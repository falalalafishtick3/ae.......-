package dev.aether.setting;

import com.google.gson.*;
import net.minecraft.util.math.MathHelper;

public class ColorSetting extends Setting<Integer> {
    public float h, s, b;

    public ColorSetting(String name, float h, float s, float b) {
        super(name, 0); this.h = h; this.s = s; this.b = b;
    }
    public int rgb() { return MathHelper.hsvToRgb(h, s, b) & 0xFFFFFF; }
    @Override public Integer get() { return rgb(); }
    public JsonElement save() {
        JsonArray a = new JsonArray(); a.add(h); a.add(s); a.add(b); return a;
    }
    public void load(JsonElement e) {
        JsonArray a = e.getAsJsonArray(); h = a.get(0).getAsFloat(); s = a.get(1).getAsFloat(); b = a.get(2).getAsFloat();
    }
}
