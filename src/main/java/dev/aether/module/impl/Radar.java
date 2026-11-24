package dev.aether.module.impl;

import dev.aether.hud.HudUtil;
import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;

public class Radar extends ClientModule {
    private final ModeSetting corner = add(new ModeSetting("Position", "Top Left", "Top Right", "Bottom Left", "Bottom Right"));
    private final NumberSetting offX = add(new NumberSetting("Offset X", 6, 0, 400, 1));
    private final NumberSetting offY = add(new NumberSetting("Offset Y", 40, 0, 400, 1));
    private final NumberSetting size = add(new NumberSetting("Size", 90, 50, 180, 2));
    private final NumberSetting range = add(new NumberSetting("Range", 40, 10, 128, 1));
    private final BoolSetting players = add(new BoolSetting("Players", true));
    private final BoolSetting hostiles = add(new BoolSetting("Hostiles", true));
    private final BoolSetting passives = add(new BoolSetting("Passives", false));

    public Radar() { super("Radar", "Mini-map of nearby entities", Category.HUD); }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int sz = size.i();
        int[] p = HudUtil.pos(corner.get(), offX.i(), offY.i(), sz, sz, g.getScaledWindowWidth(), g.getScaledWindowHeight());
        int cx = p[0] + sz / 2, cy = p[1] + sz / 2;
        int ac = ClickGuiModule.accent();

        dev.aether.gui.Ui.shadow(g, p[0], p[1], sz, sz, 6, 36);
        dev.aether.gui.Ui.rrect(g, p[0] - 1, p[1] - 1, sz + 2, sz + 2, 7, 0x22FFFFFF);
        dev.aether.gui.Ui.rrect(g, p[0], p[1], sz, sz, 6, 0xB00D1015);
        g.fill(cx, p[1], cx + 1, p[1] + sz, 0x28FFFFFF);
        g.fill(p[0], cy, p[0] + sz, cy + 1, 0x28FFFFFF);

        double yaw = Math.toRadians(mc.player.getYaw());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        double k = (sz / 2.0 - 3) / range.get();

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity)) continue;
            int col;
            if (e instanceof PlayerEntity) { if (!players.get()) continue; col = 0xFFFF4D4D; }
            else if (e instanceof Monster) { if (!hostiles.get()) continue; col = 0xFFFFA030; }
            else if (e instanceof PassiveEntity) { if (!passives.get()) continue; col = 0xFF5CDB5C; }
            else continue;
            double dx = e.getX() - mc.player.getX(), dz = e.getZ() - mc.player.getZ();
            double sx = (dx * rx + dz * rz) * k, sy = (dx * fx + dz * fz) * k;
            if (Math.abs(sx) > sz / 2.0 - 2 || Math.abs(sy) > sz / 2.0 - 2) continue;
            int x = cx + (int) sx, y = cy - (int) sy;
            g.fill(x - 1, y - 1, x + 2, y + 2, col);
        }
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
    }
}
