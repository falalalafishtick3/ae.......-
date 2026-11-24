package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;

/** Scales knockback you receive from the server (logic lives in ClientPlayNetworkHandlerMixin). */
public class Velocity extends ClientModule {
    public static Velocity INSTANCE;
    private final NumberSetting horizontal = add(new NumberSetting("Horizontal %", 0, 0, 100, 1));
    private final NumberSetting vertical   = add(new NumberSetting("Vertical %", 0, 0, 100, 1));

    public Velocity() { super("Velocity", "Reduces knockback", Category.COMBAT); INSTANCE = this; }
    public double h() { return horizontal.get() / 100.0; }
    public double v() { return vertical.get() / 100.0; }
}
