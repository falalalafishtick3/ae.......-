package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import java.util.HashMap;
import java.util.Map;

/** Toast + chat when armor or your held tool is almost broken. */
public class DurabilityAlert extends ClientModule {
    private final NumberSetting percent = add(new NumberSetting("Warn Below %", 12, 1, 50, 1));
    private final NumberSetting repeat = add(new NumberSetting("Repeat After (s)", 60, 10, 600, 10));
    private final BoolSetting chat = add(new BoolSetting("Chat Message", true));
    private final Map<String, Long> last = new HashMap<>();
    private int t;

    public DurabilityAlert() { super("DurabilityAlert", "Warns about almost-broken gear", Category.HUD); }

    @Override public void onDisable() { last.clear(); }

    private void check(MinecraftClient mc, String key, ItemStack s) {
        if (s.isEmpty() || !s.isDamageable()) return;
        float f = 1f - s.getDamage() / (float) s.getMaxDamage();
        if (f * 100f > percent.f()) { last.remove(key); return; }
        long now = System.currentTimeMillis();
        Long l = last.get(key);
        if (l != null && now - l < repeat.i() * 1000L) return;
        last.put(key, now);
        String msg = s.getName().getString() + " at " + Math.round(f * 100) + "%";
        Notifications.pushText("Low durability", msg);
        if (chat.get()) mc.player.sendMessage(Text.literal("[Aether] Low durability: " + msg), false);
    }

    @Override public void onTick(MinecraftClient mc) {
        if (++t % 40 != 0) return;
        var p = mc.player;
        check(mc, "head", p.getEquippedStack(EquipmentSlot.HEAD));
        check(mc, "chest", p.getEquippedStack(EquipmentSlot.CHEST));
        check(mc, "legs", p.getEquippedStack(EquipmentSlot.LEGS));
        check(mc, "feet", p.getEquippedStack(EquipmentSlot.FEET));
        check(mc, "main", p.getMainHandStack());
    }
}
