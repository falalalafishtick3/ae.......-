package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;

public class ClickGuiModule extends ClientModule {
    public static ClickGuiModule INSTANCE;
    private final ModeSetting theme = add(new ModeSetting("Theme", "Light Blue", "Teal", "Ice", "Purple", "Sunset", "Rose", "Mono", "Custom"));
    private final BoolSetting safeClicks = add(new BoolSetting("Safe Inventory Clicks (only when still)", true));
    private final BoolSetting perServer = add(new BoolSetting("Per-Server Config", false));
    private final ColorSetting accent = add(new ColorSetting("Accent", 0.55f, 0.55f, 1f));
    private final NumberSetting dim = add(new NumberSetting("Background Dim", 35, 0, 80, 1));

    private final ModeSetting menuBg = add(new ModeSetting("Menu Background", "Winter: All Menus", "Winter: Title Only", "Vanilla"));
    private final ModeSetting quality = add(new ModeSetting("Background Quality", "Medium", "High", "Low"));
    private final NumberSetting scanBudget = add(new NumberSetting("Scan Budget (ms/tick)", 1.0, 0.2, 5.0, 0.1));
    private final BoolSetting winter = add(new BoolSetting("Winter In GUI", true));
    private final NumberSetting snowAmount = add(new NumberSetting("Snow Amount", 140, 0, 500, 5));
    private final NumberSetting snowSpeed = add(new NumberSetting("Snow Speed", 1.0, 0.2, 3.0, 0.1));
    private final BoolSetting aurora = add(new BoolSetting("Aurora", true));
    private final BoolSetting startupSound = add(new BoolSetting("Startup Sound", true));
    private final NumberSetting startupVol = add(new NumberSetting("Startup Volume", 80, 0, 100, 1));

    public ClickGuiModule() {
        super("ClickGUI", "GUI appearance", Category.CLIENT);
        INSTANCE = this;
        accent.visibleWhen(() -> theme.is("Custom"));
        snowAmount.visibleWhen(winter::get);
        snowSpeed.visibleWhen(winter::get);
        aurora.visibleWhen(winter::get);
    }
    private static final String[] THEMES = {"Light Blue", "Teal", "Ice", "Purple", "Sunset", "Rose", "Mono"};
    private static final float[] HUES = {0.55f, 0.46f, 0.58f, 0.76f, 0.04f, 0.92f, 0.0f};
    private static final float[] SATS = {0.50f, 0.9f, 0.75f, 0.7f, 0.85f, 0.65f, 0.0f};
    private static final float[] BRS = {1.0f, 0.75f, 0.9f, 0.9f, 1f, 1f, 0.85f};

    private static int themeIndex() {
        if (INSTANCE == null) return 0;
        for (int i = 0; i < THEMES.length; i++) if (THEMES[i].equals(INSTANCE.theme.get())) return i;
        return -1; // custom
    }
    public static int accent() {
        int i = themeIndex();
        if (i < 0) return 0xFF000000 | INSTANCE.accent.rgb();
        return 0xFF000000 | (net.minecraft.util.math.MathHelper.hsvToRgb(HUES[i], SATS[i], BRS[i]) & 0xFFFFFF);
    }
    /** Base hue for animated UI (banner etc.). */
    public static float baseHue() { int i = themeIndex(); return i < 0 ? (INSTANCE == null ? 0.55f : INSTANCE.accent.h) : HUES[i]; }
    /** Saturation for animated UI text (0 for Mono). */
    public static float sat() { int i = themeIndex(); return i == 6 ? 0f : (i == 0 ? 0.5f : 0.62f); }
    public static boolean safeClicks() { return INSTANCE == null || INSTANCE.safeClicks.get(); }
    public static boolean perServer() { return INSTANCE != null && INSTANCE.perServer.get(); }
    public static String menuMode() { return INSTANCE == null ? "Winter: All Menus" : INSTANCE.menuBg.get(); }
    /** 0 = High, 1 = Medium, 2 = Low. */
    public static int quality() { return INSTANCE == null ? 1 : INSTANCE.quality.is("High") ? 0 : INSTANCE.quality.is("Medium") ? 1 : 2; }
    public static long scanBudgetNs() { return INSTANCE == null ? 1_000_000L : (long) (INSTANCE.scanBudget.get() * 1_000_000.0); }
    public static boolean winter() { return INSTANCE == null || INSTANCE.winter.get(); }
    public static int snowAmount() { return INSTANCE == null ? 140 : INSTANCE.snowAmount.i(); }
    public static float snowSpeed() { return INSTANCE == null ? 1f : INSTANCE.snowSpeed.f(); }
    public static boolean aurora() { return INSTANCE == null || INSTANCE.aurora.get(); }
    public static boolean startupSoundEnabled() { return INSTANCE == null || INSTANCE.startupSound.get(); }
    public static float startupVolume() { return INSTANCE == null ? 0.8f : INSTANCE.startupVol.f() / 100f; }
    public static int dimARGB() { return ((int) (INSTANCE.dim.get() / 100.0 * 255)) << 24; }
}
