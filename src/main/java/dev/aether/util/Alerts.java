package dev.aether.util;

import dev.aether.module.impl.HuntAlerts;
import dev.aether.module.impl.Notifications;
import net.minecraft.client.MinecraftClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/** Central "something was found" hook: toast, sound, clipboard, Discord webhook (all optional, see HuntAlerts). */
public final class Alerts {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public static String webhookUrl() {
        try {
            Path p = Store.dir().resolve("webhook.txt");
            if (!Files.exists(p)) { Files.writeString(p, "# Paste a Discord webhook URL on the line below to get pinged when something is found\n"); return ""; }
            for (String l : Files.readAllLines(p)) { l = l.trim(); if (!l.isEmpty() && !l.startsWith("#")) return l; }
        } catch (Exception ignored) {}
        return "";
    }

    public static void fire(MinecraftClient mc, String title, int x, int z, int score) {
        HuntAlerts h = HuntAlerts.INSTANCE;
        if (h == null || !h.enabled) return;
        String where = x + ", " + z;
        if (h.toast()) Notifications.pushText(title, where + (score > 0 ? "  (score " + score + ")" : ""));
        if (h.sound() && mc.getSoundManager() != null)
            mc.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.ui(net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.4f, 1.0f));
        if (h.copy()) mc.keyboard.setClipboard(x + " " + z);
        if (h.screenshot()) mc.execute(() -> net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(mc.runDirectory, mc.getFramebuffer(), t -> {}));
        if (h.webhook()) {
            String url = webhookUrl();
            if (url.isEmpty()) return;
            String server = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "singleplayer";
            String dim = mc.world != null ? mc.world.getRegistryKey().getValue().getPath() : "?";
            String msg = title + " at " + where + " (" + dim + ", " + server + ")" + (score > 0 ? " score " + score : "");
            String body = "{\"content\":\"" + msg.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
            try {
                HTTP.sendAsync(HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(8)).POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) {}
        }
    }
}
