package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;

/** Client-side interaction range (logic lives in PlayerEntityMixin). Servers enforce their own limit. */
public class Reach extends ClientModule {
    public static Reach INSTANCE;
    private final NumberSetting entity = add(new NumberSetting("Entity Reach", 3.5, 3, 6, 0.1));
    private final NumberSetting block  = add(new NumberSetting("Block Reach", 4.5, 4.5, 7, 0.1));

    public Reach() { super("Reach", "Extends your interaction range", Category.COMBAT); INSTANCE = this; }
    public double entityRange() { return entity.get(); }
    public double blockRange() { return block.get(); }
}
