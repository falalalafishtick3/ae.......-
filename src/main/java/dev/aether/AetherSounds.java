package dev.aether;

import dev.aether.module.impl.ClickGuiModule;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class AetherSounds {
    // Not registered in the game registry on purpose (keeps the mod client-only and server-compatible).
    public static final SoundEvent STARTUP = SoundEvent.of(Identifier.of("aether", "startup"));
    private static boolean played;

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (played || !(screen instanceof TitleScreen)) return;
            played = true;
            if (!ClickGuiModule.startupSoundEnabled()) return;
            client.getSoundManager().play(PositionedSoundInstance.ui(STARTUP, 1.0f, ClickGuiModule.startupVolume()));
        });
    }
}
