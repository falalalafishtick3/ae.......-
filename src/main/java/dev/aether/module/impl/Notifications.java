package dev.aether.module.impl;

import dev.aether.hud.HudUtil;
import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import java.util.ArrayList;
import java.util.List;

/** Slide-in toasts when you toggle a module. */
public class Notifications extends ClientModule {
    private record Note(String name, String detail, boolean on, long time) {}
    private static final List<Note> NOTES = new ArrayList<>();
    private static Notifications instance;

    private final ModeSetting corner = add(new ModeSetting("Position", "Bottom Right", "Top Right", "Top Left", "Bottom Left"));
    private final NumberSetting offX = add(new NumberSetting("Offset X", 8, 0, 300, 1));
    private final NumberSetting offY = add(new NumberSetting("Offset Y", 40, 0, 300, 1));
    private final NumberSetting seconds = add(new NumberSetting("Duration (s)", 2.5, 1, 8, 0.5));

    public Notifications() {
        super("Notifications", "Toasts when modules toggle", Category.HUD);
        instance = this;
        enabled = true;
    }

    public static void push(ClientModule m, boolean on) {
        if (instance == null || !instance.enabled || m == instance || m.category == Category.CLIENT) return;
        synchronized (NOTES) { NOTES.add(new Note(m.name, on ? "Enabled" : "Disabled", on, System.currentTimeMillis())); if (NOTES.size() > 6) NOTES.remove(0); }
    }

    /** Free-text toast (used by hunting alerts). */
    public static void pushText(String title, String detail) {
        if (instance == null || !instance.enabled) return;
        synchronized (NOTES) { NOTES.add(new Note(title, detail, true, System.currentTimeMillis())); if (NOTES.size() > 6) NOTES.remove(0); }
    }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        var tr = MinecraftClient.getInstance().textRenderer;
        long now = System.currentTimeMillis();
        long life = (long) (seconds.get() * 1000);
        int sw = g.getScaledWindowWidth(), sh = g.getScaledWindowHeight();
        boolean right = corner.get().endsWith("Right"), bottom = corner.get().startsWith("Bottom");
        int w = 118, h = 22, idx = 0;
        synchronized (NOTES) {
            NOTES.removeIf(n -> now - n.time() > life);
            for (int i = NOTES.size() - 1; i >= 0; i--) {
                Note n = NOTES.get(i);
                long age = now - n.time();
                float in = Math.min(1f, age / 220f), out = Math.min(1f, (life - age) / 220f);
                float vis = Math.min(in, out);
                float ease = 1f - (1f - vis) * (1f - vis);
                int slide = (int) ((1f - ease) * (w + 20));
                int[] p = HudUtil.pos(corner.get(), offX.i(), offY.i() + idx * (h + 4), w, h, sw, sh);
                int x = right ? p[0] + slide : p[0] - slide;
                int y = p[1];
                int accent = n.on() ? 0xFF4ADE80 : 0xFFF87171;
                dev.aether.hud.HudUtil.card(g, x, y, w, h, accent);
                g.drawText(tr, n.name(), x + 8, y + 3, 0xFFFFFFFF, true);
                g.drawText(tr, tr.trimToWidth(n.detail(), w - 12), x + 8, y + 12, accent, false);
                idx++;
            }
        }
    }
}
