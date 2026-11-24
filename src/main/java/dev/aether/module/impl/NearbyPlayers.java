package dev.aether.module.impl;

import dev.aether.gui.Ui;
import dev.aether.hud.HudUtil;
import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.Friends;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** List of players in render distance: name, distance, health bar. */
public class NearbyPlayers extends ClientModule {
    private final ModeSetting corner = add(new ModeSetting("Position", "Top Right", "Top Left", "Bottom Left", "Bottom Right"));
    private final NumberSetting offX = add(new NumberSetting("Offset X", 6, 0, 400, 1));
    private final NumberSetting offY = add(new NumberSetting("Offset Y", 110, 0, 400, 1));
    private final NumberSetting rows = add(new NumberSetting("Rows", 6, 1, 15, 1));
    private final BoolSetting health = add(new BoolSetting("Health Bar", true));

    public NearbyPlayers() { super("NearbyPlayers", "Players around you with distance + health", Category.HUD); }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        var tr = mc.textRenderer;
        List<PlayerEntity> list = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) if (e instanceof PlayerEntity o && o != mc.player && !o.isSpectator()) list.add(o);
        if (list.isEmpty()) return;
        list.sort(Comparator.comparingDouble(o -> mc.player.distanceTo(o)));
        int n = Math.min(rows.i(), list.size()), w = 110, rh = health.get() ? 15 : 11, h = n * rh + 6;
        int[] p = HudUtil.pos(corner.get(), offX.i(), offY.i(), w, h, g.getScaledWindowWidth(), g.getScaledWindowHeight());
        HudUtil.card(g, p[0], p[1], w, h, ClickGuiModule.accent());
        int y = p[1] + 4;
        for (int i = 0; i < n; i++) {
            PlayerEntity o = list.get(i);
            String name = o.getName().getString();
            boolean f = Friends.is(name);
            String d = (int) mc.player.distanceTo(o) + "m";
            g.drawText(tr, tr.trimToWidth(name, w - tr.getWidth(d) - 18), p[0] + 7, y, f ? 0xFF7CF29A : 0xFFFFFFFF, true);
            g.drawText(tr, d, p[0] + w - 6 - tr.getWidth(d), y, 0xFF9AA7B5, false);
            if (health.get()) {
                float hp = MathHelper.clamp(o.getHealth() / Math.max(1f, o.getMaxHealth()), 0f, 1f);
                Ui.rrect(g, p[0] + 7, y + 10, w - 14, 3, 1, 0x44FFFFFF);
                Ui.rrect(g, p[0] + 7, y + 10, Math.max(2, (int) ((w - 14) * hp)), 3, 1, 0xFF000000 | MathHelper.hsvToRgb(0.33f * hp, 0.8f, 1f));
            }
            y += rh;
        }
    }
}
