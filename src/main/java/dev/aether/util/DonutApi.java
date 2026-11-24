package dev.aether.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Minimal client for the DonutSMP public API (https://api.donutsmp.net/v1).
 * Put your key (from /api in-game) in config/aether/donut_token.txt.
 * Response shapes are parsed tolerantly because the API isn't fully documented publicly.
 */
public final class DonutApi {
    public static final String BASE = "https://api.donutsmp.net/v1";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public static volatile String lastError = "";

    public static String token() {
        try {
            Path p = Store.dir().resolve("donut_token.txt");
            if (!Files.exists(p)) { Files.writeString(p, "# Paste your DonutSMP API key (run /api in-game) on the line below\n"); return ""; }
            for (String l : Files.readAllLines(p)) { l = l.trim(); if (!l.isEmpty() && !l.startsWith("#")) return l; }
        } catch (Exception ignored) {}
        return "";
    }

    private static CompletableFuture<HttpResponse<String>> send(String path, String authHeader) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Authorization", authHeader).header("Accept", "application/json")
                .timeout(Duration.ofSeconds(8)).GET().build();
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString());
    }

    /** GET a path (e.g. "/stats/Name"). Completes with the JSON body or fails with a short message. */
    public static CompletableFuture<JsonElement> get(String path) {
        String t = token();
        if (t.isEmpty()) return CompletableFuture.failedFuture(new RuntimeException("no token"));
        return send(path, "Bearer " + t).thenCompose(r -> (r.statusCode() == 401 || r.statusCode() == 403) ? send(path, t) : CompletableFuture.completedFuture(r))
                .thenApply(r -> {
                    if (r.statusCode() / 100 != 2) throw new RuntimeException("HTTP " + r.statusCode());
                    lastError = "";
                    return JsonParser.parseString(r.body());
                }).whenComplete((v, e) -> { if (e != null) lastError = String.valueOf(e.getCause() != null ? e.getCause().getMessage() : e.getMessage()); });
    }

    // ---- tolerant JSON helpers ----
    public static Double findNumber(JsonElement e, String key, int depth) {
        if (e == null || depth > 5) return null;
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            if (o.has(key)) { Double d = toNumber(o.get(key)); if (d != null) return d; }
            for (var en : o.entrySet()) { Double d = findNumber(en.getValue(), key, depth + 1); if (d != null) return d; }
        } else if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) { Double d = findNumber(x, key, depth + 1); if (d != null) return d; }
        }
        return null;
    }
    public static Double toNumber(JsonElement e) {
        try {
            if (e == null || !e.isJsonPrimitive()) return null;
            if (e.getAsJsonPrimitive().isNumber()) return e.getAsDouble();
            String s = e.getAsString().replaceAll("[^0-9.\\-]", "");
            return s.isEmpty() ? null : Double.parseDouble(s);
        } catch (Exception ex) { return null; }
    }
    public static void collectPriced(JsonElement e, java.util.List<JsonObject> out, int depth) {
        if (e == null || depth > 6) return;
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("price") && toNumber(o.get("price")) != null) out.add(o);
            for (var en : o.entrySet()) collectPriced(en.getValue(), out, depth + 1);
        } else if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) collectPriced(x, out, depth + 1);
        }
    }
    public static boolean mentions(JsonElement e, String itemPath, int depth) {
        if (e == null || depth > 3) return false;
        if (e.isJsonPrimitive()) {
            String s = e.getAsString().toLowerCase();
            return s.equals(itemPath) || s.equals("minecraft:" + itemPath) || s.replace(' ', '_').equals(itemPath);
        }
        if (e.isJsonObject()) { for (var en : e.getAsJsonObject().entrySet()) if (mentions(en.getValue(), itemPath, depth + 1)) return true; }
        else if (e.isJsonArray()) { for (JsonElement x : (JsonArray) e) if (mentions(x, itemPath, depth + 1)) return true; }
        return false;
    }

    public static String fmt(double v) {
        double a = Math.abs(v);
        if (a >= 1e9) return String.format("%.2fB", v / 1e9);
        if (a >= 1e6) return String.format("%.2fM", v / 1e6);
        if (a >= 1e3) return String.format("%.1fK", v / 1e3);
        return v == Math.floor(v) ? String.valueOf((long) v) : String.format("%.2f", v);
    }
}
