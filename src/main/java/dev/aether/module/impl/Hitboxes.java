package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;

/** Makes targets easier to hit (client-side targeting margin; logic in EntityMixin). */
public class Hitboxes extends ClientModule {
    public static Hitboxes INSTANCE;
    private final NumberSetting expand = add(new NumberSetting("Expand", 0.2, 0.0, 1.0, 0.05));
    private final BoolSetting players  = add(new BoolSetting("Players", true));
    private final BoolSetting hostiles = add(new BoolSetting("Hostiles", true));
    private final BoolSetting passives = add(new BoolSetting("Passives", false));

    public Hitboxes() { super("Hitboxes", "Bigger target hitboxes", Category.COMBAT); INSTANCE = this; }
    public float expand() { return expand.f(); }
    public boolean applies(Entity e) {
        return (e instanceof PlayerEntity && players.get()) || (e instanceof Monster && hostiles.get())
                || (e instanceof PassiveEntity && passives.get());
    }
}
