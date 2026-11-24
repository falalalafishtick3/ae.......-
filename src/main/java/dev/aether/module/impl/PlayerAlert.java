package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerAlert extends ClientModule {
    private final BoolSetting sound = add(new BoolSetting("Sound", true));
    private final NumberSetting range = add(new NumberSetting("Range", 128, 16, 512, 8));
    private final Set<UUID> seen = new HashSet<>();

    public PlayerAlert() { super("PlayerAlert", "Alerts when a player comes into range", Category.WORLD); }

    @Override public void onDisable() { seen.clear(); }

    @Override public void onTick(MinecraftClient mc) {
        Set<UUID> now = new HashSet<>();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof PlayerEntity o) || o == mc.player || mc.player.distanceTo(o) > range.get()) continue;
            now.add(o.getUuid());
            if (seen.add(o.getUuid())) {
                mc.player.sendMessage(Text.literal(String.format("[Aether] %s came into range (%.0f blocks)", o.getName().getString(), mc.player.distanceTo(o))), false);
                if (sound.get()) mc.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.2f, 1.0f));
            }
        }
        seen.retainAll(now);
    }
}
