package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.util.Identifier;
import java.util.function.BooleanSupplier;

/** Hide vanilla HUD pieces you don't want (scoreboard, boss bars, effects icons, titles, action bar...). */
public class HudTweaks extends ClientModule {
    public static HudTweaks INSTANCE;
    private final BoolSetting scoreboard = add(new BoolSetting("Hide Scoreboard", true));
    private final BoolSetting bossBars   = add(new BoolSetting("Hide Boss Bars", false));
    private final BoolSetting effects    = add(new BoolSetting("Hide Effect Icons", false));
    private final BoolSetting titles     = add(new BoolSetting("Hide Titles", false));
    private final BoolSetting actionBar  = add(new BoolSetting("Hide Action Bar Text", false));
    private final BoolSetting heldTip    = add(new BoolSetting("Hide Held Item Name", false));
    private final BoolSetting xpLevel    = add(new BoolSetting("Hide XP Level", false));
    private final BoolSetting subtitles  = add(new BoolSetting("Hide Subtitles", false));

    public HudTweaks() { super("HudTweaks", "Hide parts of the vanilla HUD", Category.HUD); INSTANCE = this; }

    private static void wrap(Identifier id, BooleanSupplier hide) {
        HudElementRegistry.replaceElement(id, orig -> (g, tc) -> {
            if (INSTANCE != null && INSTANCE.enabled && hide.getAsBoolean()) return;
            orig.render(g, tc);
        });
    }

    /** Call once at startup (after modules exist). */
    public static void init() {
        wrap(VanillaHudElements.SCOREBOARD, () -> INSTANCE.scoreboard.get());
        wrap(VanillaHudElements.BOSS_BAR, () -> INSTANCE.bossBars.get());
        wrap(VanillaHudElements.STATUS_EFFECTS, () -> INSTANCE.effects.get());
        wrap(VanillaHudElements.TITLE_AND_SUBTITLE, () -> INSTANCE.titles.get());
        wrap(VanillaHudElements.OVERLAY_MESSAGE, () -> INSTANCE.actionBar.get());
        wrap(VanillaHudElements.HELD_ITEM_TOOLTIP, () -> INSTANCE.heldTip.get());
        wrap(VanillaHudElements.EXPERIENCE_LEVEL, () -> INSTANCE.xpLevel.get());
        wrap(VanillaHudElements.SUBTITLES, () -> INSTANCE.subtitles.get());
    }
}
