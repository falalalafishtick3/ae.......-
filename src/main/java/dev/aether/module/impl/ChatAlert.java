package dev.aether.module.impl;

import com.google.gson.reflect.TypeToken;
import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.Store;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import java.util.ArrayList;
import java.util.List;

/** Toast + ping when chat mentions you, a private message arrives, or a keyword appears. Keywords: .alert add|remove|list <word> */
public class ChatAlert extends ClientModule {
    public static ChatAlert INSTANCE;
    private static List<String> words;
    private long lastAt;

    private final BoolSetting mentions = add(new BoolSetting("My Name", true));
    private final BoolSetting privates = add(new BoolSetting("Private Messages", true));
    private final BoolSetting keywords = add(new BoolSetting("Keywords", true));
    private final BoolSetting sound = add(new BoolSetting("Ping Sound", true));

    public ChatAlert() { super("ChatAlert", "Alerts for mentions, PMs and keywords", Category.CLIENT); INSTANCE = this; }

    private static synchronized List<String> ensure() {
        if (words == null) words = new ArrayList<>(Store.load("chat_keywords.json", new TypeToken<List<String>>() {}.getType(), new ArrayList<String>()));
        return words;
    }
    public static synchronized List<String> list() { return new ArrayList<>(ensure()); }
    public static synchronized void add(String w) { if (!ensure().contains(w.toLowerCase())) ensure().add(w.toLowerCase()); Store.save("chat_keywords.json", words); }
    public static synchronized boolean remove(String w) { boolean r = ensure().remove(w.toLowerCase()); Store.save("chat_keywords.json", words); return r; }

    /** Register once at startup. */
    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> { if (!overlay) handle(message.getString()); });
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, ts) -> handle(message.getString()));
    }

    private static void handle(String raw) {
        ChatAlert m = INSTANCE;
        if (m == null || !m.enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        String s = raw.toLowerCase(), me = mc.player.getName().getString().toLowerCase();
        String why = null;
        if (m.privates.get() && (s.contains("whispers to you") || s.contains("-> me]") || s.contains("-> you]") || s.contains("[pm]") || s.contains(" whispers: ")))
            why = "Private message";
        else if (m.mentions.get() && s.contains(me) && !s.startsWith("<" + me + ">") && !s.startsWith("[aether]")) why = "You were mentioned";
        else if (m.keywords.get()) for (String k : list()) if (!k.isEmpty() && s.contains(k)) { why = "Keyword: " + k; break; }
        if (why == null) return;
        long now = System.currentTimeMillis();
        if (now - m.lastAt < 1500) return;
        m.lastAt = now;
        final String title = why, body = raw.length() > 60 ? raw.substring(0, 60) : raw;
        mc.execute(() -> {
            Notifications.pushText(title, body);
            if (m.sound.get()) mc.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1.3f, 1.0f));
        });
    }
}
