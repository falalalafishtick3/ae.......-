package dev.aether.setting;

import com.google.gson.*;

public class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, boolean def) { super(name, def); }
    public JsonElement save() { return new JsonPrimitive(value); }
    public void load(JsonElement e) { value = e.getAsBoolean(); }
}
