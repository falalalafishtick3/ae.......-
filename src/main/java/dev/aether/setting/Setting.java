package dev.aether.setting;

import com.google.gson.JsonElement;
import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    public final String name;
    protected T value;
    private BooleanSupplier visible = () -> true;

    protected Setting(String name, T def) { this.name = name; this.value = def; }
    public T get() { return value; }
    public void set(T v) { value = v; }
    public boolean isVisible() { return visible.getAsBoolean(); }
    public void visibleWhen(BooleanSupplier s) { visible = s; }
    public abstract JsonElement save();
    public abstract void load(JsonElement e);
}
