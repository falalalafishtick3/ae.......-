package dev.aether.module;

import dev.aether.setting.ModeSetting;
import dev.aether.setting.Setting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import java.util.ArrayList;
import java.util.List;

public abstract class ClientModule {
    public final String name, description;
    public final Category category;
    public final List<Setting<?>> settings = new ArrayList<>();
    public boolean enabled, expanded;
    /** GLFW key code, -1 = unbound. */
    public int key = -1;
    public boolean keyWasDown;
    public boolean favorite;
    /** Toggle = press to switch on/off. Hold = active only while the key is held. */
    public final ModeSetting bindMode = new ModeSetting("Key Mode", "Toggle", "Hold");

    protected ClientModule(String name, String description, Category category) {
        this.name = name; this.description = description; this.category = category;
        settings.add(bindMode);
        bindMode.visibleWhen(() -> key >= 0);
    }
    protected <S extends Setting<?>> S add(S s) { settings.add(s); return s; }

    public void setEnabled(boolean v) { if (enabled != v) toggle(); }
    public void toggle() { enabled = !enabled; if (enabled) onEnable(); else onDisable(); dev.aether.module.impl.Notifications.push(this, enabled); }
    public void onEnable() {}
    public void onDisable() {}
    public void onTick(MinecraftClient mc) {}
    public void onHud(DrawContext g, RenderTickCounter tc) {}
    public void onRender(WorldRenderContext ctx, MinecraftClient mc) {}
}
