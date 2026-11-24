package dev.aether.module.impl;

import dev.aether.hud.HudUtil;
import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import java.util.ArrayList;
import java.util.List;

/** Counts key PvP/utility items across your whole inventory. */
public class ItemCounter extends ClientModule {
    private record Row(Item item, BoolSetting on) {}
    private final List<Row> rows = new ArrayList<>();

    private final ModeSetting corner = add(new ModeSetting("Position", "Bottom Left", "Top Left", "Top Right", "Bottom Right"));
    private final NumberSetting offX = add(new NumberSetting("Offset X", 6, 0, 400, 1));
    private final NumberSetting offY = add(new NumberSetting("Offset Y", 60, 0, 400, 1));
    private final BoolSetting hideZero = add(new BoolSetting("Hide Zero", false));

    private void row(String name, boolean def, Item item) { rows.add(new Row(item, add(new BoolSetting(name, def)))); }

    public ItemCounter() {
        super("ItemCounter", "Counts totems, crystals, apples, pearls...", Category.HUD);
        row("Totems", true, Items.TOTEM_OF_UNDYING);
        row("End Crystals", true, Items.END_CRYSTAL);
        row("Golden Apples", true, Items.GOLDEN_APPLE);
        row("Enchanted Apples", false, Items.ENCHANTED_GOLDEN_APPLE);
        row("Ender Pearls", true, Items.ENDER_PEARL);
        row("XP Bottles", false, Items.EXPERIENCE_BOTTLE);
        row("Respawn Anchors", false, Items.RESPAWN_ANCHOR);
        row("Glowstone", false, Items.GLOWSTONE);
        row("Fireworks", false, Items.FIREWORK_ROCKET);
        row("Arrows", false, Items.ARROW);
    }

    private int count(MinecraftClient mc, Item item) {
        int n = 0;
        for (int i = 0; i < 36; i++) { ItemStack s = mc.player.getInventory().getStack(i); if (s.isOf(item)) n += s.getCount(); }
        ItemStack off = mc.player.getOffHandStack();
        if (off.isOf(item)) n += off.getCount();
        return n;
    }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        var tr = mc.textRenderer;
        List<Row> show = new ArrayList<>(); List<Integer> counts = new ArrayList<>();
        for (Row r : rows) {
            if (!r.on().get()) continue;
            int c = count(mc, r.item());
            if (hideZero.get() && c == 0) continue;
            show.add(r); counts.add(c);
        }
        if (show.isEmpty()) return;
        int w = 44, h = show.size() * 18 + 4;
        int[] p = HudUtil.pos(corner.get(), offX.i(), offY.i(), w, h, g.getScaledWindowWidth(), g.getScaledWindowHeight());
        HudUtil.card(g, p[0], p[1], w, h, ClickGuiModule.accent());
        int y = p[1] + 2;
        for (int i = 0; i < show.size(); i++) {
            g.drawItem(new ItemStack(show.get(i).item()), p[0] + 5, y);
            int c = counts.get(i);
            g.drawText(tr, "x" + c, p[0] + 25, y + 5, c == 0 ? 0xFFF87171 : 0xFFFFFFFF, true);
            y += 18;
        }
    }
}
