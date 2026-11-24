package dev.aether.setting;

import com.google.gson.*;

public class NumberSetting extends Setting<Double> {
    public final double min, max, step;

    public NumberSetting(String name, double def, double min, double max, double step) {
        super(name, def); this.min = min; this.max = max; this.step = step;
    }
    @Override public void set(Double v) {
        double c = Math.max(min, Math.min(max, v));
        value = Math.round(c / step) * step;
    }
    public int i() { return (int) Math.round(value); }
    public float f() { return value.floatValue(); }
    public JsonElement save() { return new JsonPrimitive(value); }
    public void load(JsonElement e) { set(e.getAsDouble()); }
}
