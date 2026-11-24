package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.InvUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.EntityHitResult;

/** Switches to your best weapon when you attack something. */
public class AutoWeapon extends ClientModule {
    private static final String[] TIERS = {"netherite", "diamond", "iron", "stone", "golden", "wooden"};
    private final ModeSetting prefer = add(new ModeSetting("Prefer", "Sword", "Axe", "Mace"));
    private final BoolSetting switchBack = add(new BoolSetting("Switch Back", true));
    private int prev = -1;

    public AutoWeapon() { super("AutoWeapon", "Auto-select your best weapon when attacking", Category.COMBAT); }

    private int score(ItemStack s) {
        if (s.isEmpty()) return -1;
        boolean ok = switch (prefer.get()) { case "Axe" -> s.isIn(ItemTags.AXES); case "Mace" -> s.isOf(Items.MACE); default -> s.isIn(ItemTags.SWORDS); };
        if (!ok) return -1;
        String id = Registries.ITEM.getId(s.getItem()).getPath();
        for (int i = 0; i < TIERS.length; i++) if (id.startsWith(TIERS[i])) return TIERS.length - i;
        return 1;
    }

    @Override public void onTick(MinecraftClient mc) {
        boolean attacking = mc.currentScreen == null && mc.options.attackKey.isPressed()
                && mc.crosshairTarget instanceof EntityHitResult r && r.getEntity() instanceof LivingEntity;
        if (!attacking) { if (prev >= 0 && switchBack.get()) InvUtil.select(mc, prev); prev = -1; return; }
        int best = -1, bs = -1;
        for (int i = 0; i < 9; i++) { int sc = score(mc.player.getInventory().getStack(i)); if (sc > bs) { bs = sc; best = i; } }
        if (best >= 0 && best != InvUtil.selected(mc)) { if (prev < 0) prev = InvUtil.selected(mc); InvUtil.select(mc, best); }
    }
}
