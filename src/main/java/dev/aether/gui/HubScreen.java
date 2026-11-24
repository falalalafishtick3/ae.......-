package dev.aether.gui;

import dev.aether.module.impl.ClickGuiModule;
import dev.aether.module.impl.PlayerLog;
import dev.aether.util.Friends;
import dev.aether.util.FoundBases;
import dev.aether.util.WaypointStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Browse saved base finds and waypoints: copy coordinates, turn a base into a waypoint, delete. */
public class HubScreen extends Screen {
    private int tab;
    private double scroll;
    private String toast = ""; private long toastAt;
    private record Hit(int x, int y, int w, int h, Runnable r) { boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; } }
    private final List<Hit> hits = new ArrayList<>();

    public HubScreen(int tab) { super(Text.literal("Aether Hub")); this.tab = tab; }

    private static String dimName(String d) { return switch (d) { case "the_end" -> "End"; case "the_nether" -> "Nether"; case "overworld" -> "Overworld"; default -> d; }; }
    private static String age(long t) {
        long s = (System.currentTimeMillis() - t) / 1000;
        return s < 90 ? s + "s ago" : s < 5400 ? (s / 60) + "m ago" : s < 129600 ? (s / 3600) + "h ago" : (s / 86400) + "d ago";
    }
    private void say(String s) { toast = s; toastAt = System.currentTimeMillis(); }
    private void copy(String s) { client.keyboard.setClipboard(s); say("Copied " + s); }

    private void btn(DrawContext g, String label, int x, int y, int w, int mx, int my, Runnable r) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + 14;
        g.fill(x, y, x + w, y + 14, hov ? 0xFF2A3440 : 0xFF161C24);
        g.fill(x, y + 13, x + w, y + 14, ClickGuiModule.accent());
        g.drawText(textRenderer, label, x + (w - textRenderer.getWidth(label)) / 2, y + 3, 0xFFFFFFFF, false);
        hits.add(new Hit(x, y, w, 14, r));
    }

    @Override public boolean shouldPause() { return false; }

    @Override public void render(DrawContext g, int mx, int my, float delta) {
        hits.clear();
        if (ClickGuiModule.winter()) dev.aether.render.WinterBackground.draw(g, width, height, false);
        g.fill(0, 0, width, height, 0x66000000);

        int pw = Math.min(width - 24, 520), px = (width - pw) / 2, top = 40, bottom = height - 34;
        g.drawText(textRenderer, "AETHER HUB", px, 14, ClickGuiModule.accent(), true);
        btn(g, "Bases (" + FoundBases.all().size() + ")", px + pw - 372, 12, 80, mx, my, () -> { tab = 0; scroll = 0; });
        btn(g, "Waypoints (" + WaypointStore.all().size() + ")", px + pw - 288, 12, 100, mx, my, () -> { tab = 1; scroll = 0; });
        btn(g, "Players (" + PlayerLog.all().size() + ")", px + pw - 184, 12, 80, mx, my, () -> { tab = 2; scroll = 0; });
        btn(g, "Friends (" + Friends.all().size() + ")", px + pw - 100, 12, 54, mx, my, () -> { tab = 3; scroll = 0; });
        btn(g, "Close", px + pw - 42, 12, 42, mx, my, this::close);

        g.fill(px, top, px + pw, bottom, 0xB0101218);
        g.fill(px, top, px + pw, top + 1, ClickGuiModule.accent());

        var mc = MinecraftClient.getInstance();
        int rowH = 24, y = top + 4 + (int) scroll;
        g.enableScissor(px, top + 1, px + pw, bottom);
        if (tab == 0) {
            List<FoundBases.Entry> list = FoundBases.all();
            list.sort((a, b) -> Long.compare(b.time, a.time));
            if (list.isEmpty()) g.drawText(textRenderer, "No bases saved yet. Run BaseHunter to find some.", px + 8, y + 6, 0xFF888899, false);
            for (FoundBases.Entry e : list) {
                g.fill(px + 4, y, px + pw - 4, y + rowH - 2, 0xFF161C24);
                g.drawText(textRenderer, e.x + ", " + e.z + "   score " + e.score + "   y " + e.minY + "-" + e.maxY, px + 10, y + 3, 0xFFFFFFFF, false);
                g.drawText(textRenderer, dimName(e.dim) + "  |  " + e.server + "  |  " + age(e.time), px + 10, y + 13, 0xFF8899AA, false);
                int bx = px + pw - 8;
                bx -= 34; final FoundBases.Entry fe = e;
                btn(g, "Del", bx, y + 4, 30, mx, my, () -> FoundBases.remove(fe));
                bx -= 34; btn(g, "WP", bx, y + 4, 30, mx, my, () -> {
                    WaypointStore.Entry w = new WaypointStore.Entry();
                    w.name = "Base " + fe.x + "," + fe.z; w.dim = fe.dim; w.x = fe.x; w.z = fe.z; w.y = (fe.minY + fe.maxY) / 2;
                    w.hue = ThreadLocalRandom.current().nextFloat(); WaypointStore.add(w); say("Added waypoint");
                });
                bx -= 40; btn(g, "Copy", bx, y + 4, 36, mx, my, () -> copy(fe.x + " " + fe.z));
                y += rowH;
            }
        } else if (tab == 1) {
            List<WaypointStore.Entry> list = WaypointStore.all();
            if (list.isEmpty()) g.drawText(textRenderer, "No waypoints. Use \"Add Here\" or bind the AddWaypoint module.", px + 8, y + 6, 0xFF888899, false);
            for (WaypointStore.Entry e : list) {
                g.fill(px + 4, y, px + pw - 4, y + rowH - 2, 0xFF161C24);
                int col = 0xFF000000 | net.minecraft.util.math.MathHelper.hsvToRgb(e.hue, 0.6f, 1f);
                g.fill(px + 4, y, px + 6, y + rowH - 2, col);
                g.drawText(textRenderer, e.name + "   " + e.x + ", " + e.y + ", " + e.z, px + 10, y + 3, 0xFFFFFFFF, false);
                String dist = "";
                if (mc.player != null && mc.world != null && mc.world.getRegistryKey().getValue().getPath().equals(e.dim))
                    dist = "  |  " + (int) Math.hypot(e.x - mc.player.getX(), e.z - mc.player.getZ()) + "m away";
                g.drawText(textRenderer, dimName(e.dim) + dist, px + 10, y + 13, 0xFF8899AA, false);
                int bx = px + pw - 8; final WaypointStore.Entry fe = e;
                bx -= 34; btn(g, "Del", bx, y + 4, 30, mx, my, () -> WaypointStore.remove(fe));
                bx -= 40; btn(g, "Copy", bx, y + 4, 36, mx, my, () -> copy(fe.x + " " + fe.y + " " + fe.z));
                y += rowH;
            }
        } else if (tab == 2) {
            List<PlayerLog.Seen> list = PlayerLog.all();
            list.sort((a, b) -> Long.compare(b.time, a.time));
            if (list.isEmpty()) g.drawText(textRenderer, "No players logged. Enable the PlayerLog module.", px + 8, y + 6, 0xFF888899, false);
            for (PlayerLog.Seen e : list) {
                g.fill(px + 4, y, px + pw - 4, y + rowH - 2, 0xFF161C24);
                boolean fr = Friends.is(e.name);
                g.drawText(textRenderer, e.name + "   " + e.x + ", " + e.y + ", " + e.z, px + 10, y + 3, fr ? 0xFF7CF29A : 0xFFFFFFFF, false);
                g.drawText(textRenderer, dimName(e.dim) + "  |  " + e.server + "  |  " + age(e.time), px + 10, y + 13, 0xFF8899AA, false);
                int bx = px + pw - 8; final PlayerLog.Seen fe = e;
                bx -= 34; btn(g, "WP", bx, y + 4, 30, mx, my, () -> {
                    WaypointStore.Entry w = new WaypointStore.Entry();
                    w.name = fe.name; w.dim = fe.dim; w.x = fe.x; w.y = fe.y; w.z = fe.z; w.hue = ThreadLocalRandom.current().nextFloat();
                    WaypointStore.add(w); say("Added waypoint");
                });
                bx -= 40; btn(g, fr ? "Unfriend" : "Friend", bx - 16, y + 4, 52, mx, my, () -> { Friends.toggle(fe.name); });
                bx -= 70; btn(g, "Copy", bx, y + 4, 36, mx, my, () -> copy(fe.x + " " + fe.y + " " + fe.z));
                y += rowH;
            }
        } else {
            List<String> list = Friends.all();
            if (list.isEmpty()) g.drawText(textRenderer, "No friends yet. Bind FriendToggle and aim at a player, or use .friend add <name>.", px + 8, y + 6, 0xFF888899, false);
            for (String name : list) {
                g.fill(px + 4, y, px + pw - 4, y + rowH - 2, 0xFF161C24);
                g.fill(px + 4, y, px + 6, y + rowH - 2, 0xFF7CF29A);
                g.drawText(textRenderer, name, px + 10, y + 8, 0xFFFFFFFF, false);
                int bx = px + pw - 8 - 50; final String fn = name;
                btn(g, "Remove", bx, y + 4, 46, mx, my, () -> Friends.toggle(fn));
                y += rowH;
            }
        }
        g.disableScissor();

        // bottom actions
        if (tab == 1 && mc.player != null && mc.world != null)
            btn(g, "Add Here", px, bottom + 8, 64, mx, my, () -> {
                WaypointStore.Entry w = new WaypointStore.Entry();
                w.name = "WP " + (WaypointStore.all().size() + 1); w.dim = mc.world.getRegistryKey().getValue().getPath();
                w.x = (int) Math.floor(mc.player.getX()); w.y = (int) Math.floor(mc.player.getY()); w.z = (int) Math.floor(mc.player.getZ());
                w.hue = ThreadLocalRandom.current().nextFloat(); WaypointStore.add(w); say("Saved " + w.name);
            });
        if (tab <= 1) btn(g, "Clear All", px + pw - 60, bottom + 8, 60, mx, my, () -> { if (tab == 0) FoundBases.clear(); else WaypointStore.clear(); });
        if (System.currentTimeMillis() - toastAt < 2000) g.drawText(textRenderer, toast, px + 80, bottom + 12, 0xFFFFC857, false);

        // clamp scroll
        int count = tab == 0 ? FoundBases.all().size() : tab == 1 ? WaypointStore.all().size() : tab == 2 ? PlayerLog.all().size() : Friends.all().size();
        double min = Math.min(0, (bottom - top - 8) - count * rowH);
        scroll = Math.max(min, Math.min(0, scroll));
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0)
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (h.in(click.x(), click.y())) { h.r().run(); return true; }
            }
        return super.mouseClicked(click, doubled);
    }
    @Override public boolean mouseScrolled(double mx, double my, double h, double v) { scroll += v * 20; return true; }
}
