package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Marks where interesting sounds happened (chests, doors, pistons, block breaking/placing, armor equip...). Hook: ClientPlayNetworkHandlerMixin. */
public class SoundESP extends EspModule {
    public static SoundESP INSTANCE;
    private record Rec(double x, double y, double z, String id, long time) {}

    private final Map<BoolSetting, String[]> groups = new LinkedHashMap<>();
    private final NumberSetting keep = add(new NumberSetting("Keep For (s)", 30, 5, 300, 5));
    private final BoolSetting chat = add(new BoolSetting("Chat Notify", true));
    private final ColorSetting color = add(new ColorSetting("Color", 0.14f, 0.8f, 1f));
    private final List<Rec> recs = new ArrayList<>();

    private void group(String name, boolean def, String... keys) { groups.put(add(new BoolSetting(name, def)), keys); }

    public SoundESP() {
        super("SoundESP", "Shows where activity sounds came from", Category.WORLD);
        INSTANCE = this;
        group("Containers", true, "chest", "barrel", "shulker_box", "ender_chest");
        group("Doors / Gates", true, "door", "trapdoor", "fence_gate");
        group("Redstone", true, "piston", "lever", "button", "pressure_plate", "dispenser", "hopper", "comparator", "repeater", "observer");
        group("Workstations", true, "anvil", "furnace", "grindstone", "smithing_table", "brewing_stand", "enchantment_table");
        group("Block Break / Place", false, ".break", ".place");
        group("Player Gear", true, "armor.equip", "elytra", "totem", "bow.shoot", "crossbow", "trident");
        group("Explosions", true, "explode", "tnt");
        addCommonSettings();
        renderMode.set("Both");
        range.set(256.0);
    }

    @Override public void onDisable() { super.onDisable(); synchronized (recs) { recs.clear(); } }

    /** Called from the network mixin on the main thread. */
    public void onSound(MinecraftClient mc, String id, double x, double y, double z) {
        boolean match = false;
        for (var e : groups.entrySet()) {
            if (!e.getKey().get()) continue;
            for (String k : e.getValue()) if (id.contains(k)) { match = true; break; }
            if (match) break;
        }
        if (!match) return;
        long now = System.currentTimeMillis();
        synchronized (recs) {
            for (Rec r : recs) if (r.id().equals(id) && Math.abs(r.x() - x) < 1 && Math.abs(r.y() - y) < 1 && Math.abs(r.z() - z) < 1 && now - r.time() < 1500) return;
            recs.add(new Rec(x, y, z, id, now));
            if (recs.size() > 200) recs.remove(0);
        }
        if (chat.get() && mc.player != null)
            mc.player.sendMessage(Text.literal(String.format("[Aether] Sound %s at %d, %d, %d", id, (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z))), false);
    }

    @Override protected void scan(MinecraftClient mc, List<Target> out) {
        long now = System.currentTimeMillis(), life = keep.i() * 1000L;
        synchronized (recs) {
            recs.removeIf(r -> now - r.time() > life);
            for (Rec r : recs) out.add(new Target(new Box(r.x() - 0.5, r.y() - 0.5, r.z() - 0.5, r.x() + 0.5, r.y() + 0.5, r.z() + 0.5), color.rgb()));
        }
    }
}
