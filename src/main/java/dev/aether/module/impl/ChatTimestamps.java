package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Prepends [HH:mm] to server/system chat messages. (Signed player chat can't be modified by the client.) */
public class ChatTimestamps extends ClientModule {
    public static ChatTimestamps INSTANCE;
    private final ModeSetting format = add(new ModeSetting("Format", "HH:mm", "HH:mm:ss", "h:mm a"));

    public ChatTimestamps() { super("ChatTimestamps", "Time before chat messages", Category.CLIENT); INSTANCE = this; }

    public static void init() {
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            ChatTimestamps m = INSTANCE;
            if (m == null || !m.enabled || overlay) return message;
            String ts = LocalTime.now().format(DateTimeFormatter.ofPattern(m.format.get()));
            return Text.literal("[" + ts + "] ").formatted(Formatting.GRAY).append(message);
        });
    }
}
