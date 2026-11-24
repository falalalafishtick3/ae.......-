package dev.aether.module.impl;

import dev.aether.hud.HudUtil;
import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import java.util.ArrayList;
import java.util.List;

public class InfoHud extends ClientModule {
    private final ModeSetting corner = add(new ModeSetting("Position", "Bottom Left", "Top Left", "Top Right", "Bottom Right"));
    private final NumberSetting offX = add(new NumberSetting("Offset X", 4, 0, 300, 1));
    private final NumberSetting offY = add(new NumberSetting("Offset Y", 4, 0, 300, 1));
    private final BoolSetting fps    = add(new BoolSetting("FPS", true));
    private final BoolSetting ping   = add(new BoolSetting("Ping", true));
    private final BoolSetting coords = add(new BoolSetting("Coordinates", true));
    private final BoolSetting facing = add(new BoolSetting("Direction", true));
    private final BoolSetting converted = add(new BoolSetting("Nether / Overworld Coords", true));
    private final BoolSetting biomeB = add(new BoolSetting("Biome", true));
    private final BoolSetting lightB = add(new BoolSetting("Light Level", false));
    private final BoolSetting speedB = add(new BoolSetting("Speed (b/s)", true));
    private final BoolSetting bg     = add(new BoolSetting("Background", true));

    private double px, pz, speed;

    public InfoHud() { super("InfoHud", "FPS, ping, coords, direction, speed", Category.HUD); }

    @Override public void onTick(MinecraftClient mc) {
        double x = mc.player.getX(), z = mc.player.getZ();
        speed = Math.hypot(x - px, z - pz) * 20.0;
        px = x; pz = z;
    }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        var tr = mc.textRenderer;
        List<String> lines = new ArrayList<>();
        if (fps.get()) lines.add("FPS: " + mc.getCurrentFps());
        if (ping.get() && mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) lines.add("Ping: " + e.getLatency() + " ms");
        }
        if (coords.get()) lines.add(String.format("XYZ: %.1f / %.1f / %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        if (converted.get()) {
            String d = mc.world.getRegistryKey().getValue().getPath();
            if (d.equals("overworld")) lines.add(String.format("Nether: %.0f / %.0f", mc.player.getX() / 8.0, mc.player.getZ() / 8.0));
            else if (d.equals("the_nether")) lines.add(String.format("Overworld: %.0f / %.0f", mc.player.getX() * 8.0, mc.player.getZ() * 8.0));
        }
        if (biomeB.get()) {
            String bn = mc.world.getBiome(mc.player.getBlockPos()).getKey().map(k -> k.getValue().getPath()).orElse("unknown");
            lines.add("Biome: " + bn.replace('_', ' '));
        }
        if (lightB.get()) lines.add("Light: " + mc.world.getLightLevel(mc.player.getBlockPos()));
        if (facing.get()) lines.add("Facing: " + mc.player.getHorizontalFacing().getId());
        if (speedB.get()) lines.add(String.format("Speed: %.2f b/s", speed));
        if (lines.isEmpty()) return;

        int w = 0;
        for (String l : lines) w = Math.max(w, tr.getWidth(l));
        int h = lines.size() * 10;
        int[] p = HudUtil.pos(corner.get(), offX.i(), offY.i(), w + 8, h + 4, g.getScaledWindowWidth(), g.getScaledWindowHeight());
        int ac = ClickGuiModule.accent();
        if (bg.get()) HudUtil.card(g, p[0], p[1], w + 8, h + 4, ac);
        int y = p[1] + 3;
        for (String l : lines) { g.drawText(tr, l, p[0] + 5, y, 0xFFFFFFFF, true); y += 10; }
    }
}
