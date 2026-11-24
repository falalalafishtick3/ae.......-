package dev.aether.hud;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Reads what the local Spotify desktop app is playing, no account/API key needed.
 *  - Windows: Spotify window title ("Artist - Song"); no progress info available.
 *  - Linux:   `playerctl` (MPRIS). Install playerctl.
 *  - macOS:   AppleScript.
 */
public final class SpotifyTracker {
    public static volatile String title = "", artist = "";
    public static volatile boolean playing, running, hasProgress;
    public static volatile long posMs, lenMs, stampMs, changedAt;
    public static volatile int intervalMs = 2000;

    private static Thread thread;
    private static volatile boolean stop;

    public static synchronized void ensureRunning() {
        if (thread != null && thread.isAlive()) return;
        stop = false;
        thread = new Thread(SpotifyTracker::loop, "Aether-Spotify");
        thread.setDaemon(true);
        thread.start();
    }
    public static synchronized void shutdown() { stop = true; thread = null; }

    /** Current position, extrapolated between polls while playing. */
    public static long position() {
        long p = posMs + (playing ? System.currentTimeMillis() - stampMs : 0);
        return lenMs > 0 ? Math.min(p, lenMs) : p;
    }

    private static void loop() {
        while (!stop) {
            try { poll(); } catch (Throwable ignored) { running = false; }
            try { Thread.sleep(Math.max(500, intervalMs)); } catch (InterruptedException e) { return; }
        }
    }

    private static void poll() throws Exception {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) pollWindows();
        else if (os.contains("mac")) pollMac();
        else pollLinux();
    }

    private static String run(String... cmd) throws Exception {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        if (!p.waitFor(4, TimeUnit.SECONDS)) { p.destroyForcibly(); return null; }
        return new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
    }

    private static void set(String a, String t, boolean pl, long pos, long len, boolean prog) {
        if (!t.equals(title) || !a.equals(artist)) changedAt = System.currentTimeMillis();
        artist = a; title = t; playing = pl; posMs = pos; lenMs = len; hasProgress = prog;
        stampMs = System.currentTimeMillis(); running = true;
    }

    private static void pollWindows() throws Exception {
        // tasklist is far lighter than spawning PowerShell; the last CSV column is the window title.
        String out = run("tasklist", "/v", "/fi", "IMAGENAME eq Spotify.exe", "/fo", "csv", "/nh");
        if (out == null || out.isEmpty() || !out.toLowerCase().contains("spotify.exe")) { running = false; playing = false; return; }
        String t = "";
        for (String line : out.split("\\r?\\n")) {
            if (!line.toLowerCase().startsWith("\"spotify.exe\"")) continue;
            int k = line.lastIndexOf("\",\"");
            if (k < 0) continue;
            String title = line.substring(k + 3).replaceAll("\"$", "").trim();
            if (!title.isEmpty() && !title.equalsIgnoreCase("N/A") && !title.equals("Spotify") && !title.startsWith("Spotify Free")
                    && !title.startsWith("Spotify Premium") && !title.contains("MSCTFIME") && !title.contains("Default IME") && !title.contains("GDI+")) {
                t = title; break;
            }
        }
        if (t.isEmpty() || t.equals("Advertisement")) { running = true; playing = false; return; }
        int i = t.indexOf(" - ");
        if (i < 0) { set("", t, true, 0, 0, false); return; }
        set(t.substring(0, i), t.substring(i + 3), true, 0, 0, false);
    }

    private static void pollLinux() throws Exception {
        String out = run("playerctl", "-p", "spotify", "metadata", "--format",
                "{{status}}\t{{artist}}\t{{title}}\t{{position}}\t{{mpris:length}}");
        if (out == null || out.isEmpty() || out.contains("No players") || out.contains("not found")) { running = false; playing = false; return; }
        String[] f = out.split("\t", -1);
        if (f.length < 3) { running = false; return; }
        long pos = f.length > 3 ? parse(f[3]) / 1000 : 0, len = f.length > 4 ? parse(f[4]) / 1000 : 0;
        set(f[1], f[2], f[0].equalsIgnoreCase("Playing"), pos, len, len > 0);
    }

    private static void pollMac() throws Exception {
        String out = run("osascript", "-e",
                "if application \"Spotify\" is running then\n"
              + "tell application \"Spotify\" to return (player state as text) & \"|\" & (artist of current track) & \"|\" & (name of current track) & \"|\" & (player position as text) & \"|\" & (duration of current track as text)\n"
              + "else\nreturn \"\"\nend if");
        if (out == null || out.isEmpty()) { running = false; playing = false; return; }
        String[] f = out.split("\\|", -1);
        if (f.length < 5) { running = false; return; }
        long pos = (long) (parseD(f[3]) * 1000), len = (long) parseD(f[4]);
        set(f[1], f[2], f[0].trim().equalsIgnoreCase("playing"), pos, len, len > 0);
    }

    private static long parse(String s) { try { return Long.parseLong(s.trim().split("\\.")[0]); } catch (Exception e) { return 0; } }
    private static double parseD(String s) { try { return Double.parseDouble(s.trim().replace(',', '.')); } catch (Exception e) { return 0; } }
}
