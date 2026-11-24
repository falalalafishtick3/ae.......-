package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.Alerts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Alerts when valuable items lie on the ground in range (death drops, stash spills, mob farms...). */
public class LootAlert extends ClientModule {
    private final Map<BoolSetting, String[]> groups = new LinkedHashMap<>();
    private final NumberSetting range = add(new NumberSetting("Range", 128, 16, 400, 8));
    private final Set<Integer> seen = new HashSet<>();
    private int tick;

    private void group(String name, boolean def, String... keys) { groups.put(add(new BoolSetting(name, def)), keys); }

    public LootAlert() {
        super("LootAlert", "Alerts for valuable dropped items", Category.WORLD);
        group("Netherite / Ancient Debris", true, "netherite", "ancient_debris");
        group("Elytra / Totems", true, "elytra", "totem_of_undying");
        group("Shulker Boxes", true, "shulker_box");
        group("Enchanted Golden Apple", true, "enchanted_golden_apple");
        group("Diamonds / Emeralds", false, "diamond", "emerald");
        group("Rare", true, "nether_star", "beacon", "dragon_egg", "trident", "mace", "heavy_core", "spawner");
    }

    @Override public void onDisable() { seen.clear(); }

    @Override public void onTick(MinecraftClient mc) {
        if (++tick % 20 != 0) return;
        double r2 = range.get() * range.get();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof ItemEntity it) || seen.contains(e.getId())) continue;
            double dx = e.getX() - mc.player.getX(), dz = e.getZ() - mc.player.getZ();
            if (dx * dx + dz * dz > r2) continue;
            String id = Registries.ITEM.getId(it.getStack().getItem()).getPath();
            boolean hit = false;
            for (var g : groups.entrySet()) {
                if (!g.getKey().get()) continue;
                for (String k : g.getValue()) if (id.contains(k)) { hit = true; break; }
                if (hit) break;
            }
            if (!hit) continue;
            seen.add(e.getId());
            int x = (int) Math.floor(e.getX()), z = (int) Math.floor(e.getZ());
            mc.player.sendMessage(Text.literal("[Aether] Loot: " + it.getStack().getCount() + "x " + it.getStack().getName().getString()
                    + " at " + x + ", " + (int) e.getY() + ", " + z), false);
            Alerts.fire(mc, "Loot: " + it.getStack().getName().getString(), x, z, 0);
        }
        if (seen.size() > 600) seen.clear();
    }
}
