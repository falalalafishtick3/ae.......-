package dev.aether.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Store {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Path dir() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("aether");
        try { Files.createDirectories(p); } catch (Exception ignored) {}
        return p;
    }
    public static <T> T load(String file, Type type, T fallback) {
        try {
            Path p = dir().resolve(file);
            if (!Files.exists(p)) return fallback;
            T v = GSON.fromJson(Files.readString(p), type);
            return v == null ? fallback : v;
        } catch (Exception e) { return fallback; }
    }
    public static void save(String file, Object o) {
        try { Files.writeString(dir().resolve(file), GSON.toJson(o)); } catch (Exception ignored) {}
    }
}
