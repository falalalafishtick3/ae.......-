package dev.aether.module.impl;

import com.google.gson.JsonObject;
import dev.aether.hud.HudUtil;
import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.DonutApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DonutSMP economy helpers: auction price tooltips, inventory value and your money/shards on the HUD.
 * Needs your API key in config/aether/donut_token.txt. (Experimental: the public API's response format is parsed tolerantly.)
 */
public class DonutEconomy extends ClientModule {
    public static DonutEconomy INSTANCE;

    private static final class Price { volatile double unit = -1; volatile long at; volatile boolean pending, failed; }
    private static final Map<String, Price> CACHE = new ConcurrentHashMap<>();
    private static final AtomicInteger INFLIGHT = new AtomicInteger();

    private final BoolSetting tooltips = add(new BoolSetting("Price Tooltips", true));
    private final BoolSetting showInv  = add(new BoolSetting("Inventory Value", true));
    private final BoolSetting showStats = add(new BoolSetting("Money / Shards", true));
    private final NumberSetting refresh = add(new NumberSetting("Stats Refresh (s)", 60, 30, 600, 5));
    private final NumberSetting cacheMin = add(new NumberSetting("Price Cache (min)", 3, 1, 30, 1));
    private final ModeSetting corner = add(new ModeSetting("Position", "Top Left", "Top Right", "Bottom Left", "Bottom Right"));
    private final NumberSetting offX = add(new NumberSetting("Offset X", 6, 0, 300, 1));
    private final NumberSetting offY = add(new NumberSetting("Offset Y", 190, 0, 300, 1));

    private volatile double money = -1, shards = -1;
    private volatile boolean statsPending;
    private long lastStats;
    private double invValue; private int invPriced, invItems;
    private int tick;

    public DonutEconomy() { super("DonutEconomy", "Auction prices, inventory value, money/shards (DonutSMP)", Category.HUD); INSTANCE = this; }

    // ---------------- prices ----------------
    private static Price price(String id) {
        Price p = CACHE.computeIfAbsent(id, k -> new Price());
        long ttl = (INSTANCE == null ? 3 : INSTANCE.cacheMin.i()) * 60_000L;
        if (!p.pending && System.currentTimeMillis() - p.at > ttl && INFLIGHT.get() < 3) fetch(id, p);
        return p;
    }

    private static void fetch(String id, Price p) {
        p.pending = true; INFLIGHT.incrementAndGet();
        String q = URLEncoder.encode(id, StandardCharsets.UTF_8);
        DonutApi.get("/auction/list/1?search=" + q + "&sort=lowest_price").whenComplete((json, err) -> {
            try {
                if (err != null || json == null) { p.failed = true; return; }
                List<JsonObject> listings = new ArrayList<>();
                DonutApi.collectPriced(json, listings, 0);
                double best = Double.MAX_VALUE;
                for (JsonObject o : listings) {
                    if (!DonutApi.mentions(o, id, 0)) continue;
                    Double pr = DonutApi.toNumber(o.get("price"));
                    Double cnt = DonutApi.findNumber(o, "count", 0);
                    if (cnt == null) cnt = DonutApi.findNumber(o, "amount", 0);
                    if (cnt == null) cnt = DonutApi.findNumber(o, "quantity", 0);
                    double unit = pr / Math.max(1.0, cnt == null ? 1.0 : cnt);
                    if (unit < best) best = unit;
                }
                p.unit = best == Double.MAX_VALUE ? -1 : best;
                p.failed = false;
            } finally { p.at = System.currentTimeMillis(); p.pending = false; INFLIGHT.decrementAndGet(); }
        });
    }

    public static void tooltip(ItemStack stack, List<Text> lines) {
        DonutEconomy m = INSTANCE;
        if (m == null || !m.enabled || !m.tooltips.get() || stack.isEmpty() || DonutApi.token().isEmpty()) return;
        String id = Registries.ITEM.getId(stack.getItem()).getPath();
        Price p = price(id);
        if (p.unit >= 0) {
            lines.add(Text.literal("Price: " + DonutApi.fmt(p.unit) + " each").formatted(Formatting.GOLD));
            if (stack.getCount() > 1) lines.add(Text.literal("Stack: " + DonutApi.fmt(p.unit * stack.getCount())).formatted(Formatting.YELLOW));
        } else if (p.pending || p.at == 0) lines.add(Text.literal("Price: loading...").formatted(Formatting.GRAY));
        else lines.add(Text.literal(p.failed ? "Price: unavailable (" + DonutApi.lastError + ")" : "Price: no listings").formatted(Formatting.GRAY));
    }

    // ---------------- stats / inventory ----------------
    @Override public void onTick(MinecraftClient mc) {
        if (DonutApi.token().isEmpty()) return;
        long now = System.currentTimeMillis();
        if (showStats.get() && !statsPending && now - lastStats > refresh.i() * 1000L) {
            lastStats = now; statsPending = true;
            String name = mc.player.getName().getString();
            DonutApi.get("/stats/" + URLEncoder.encode(name, StandardCharsets.UTF_8)).whenComplete((json, err) -> {
                statsPending = false;
                if (err != null || json == null) return;
                Double m = DonutApi.findNumber(json, "money", 0), s = DonutApi.findNumber(json, "shards", 0);
                if (m != null) money = m;
                if (s != null) shards = s;
            });
        }
        if (showInv.get() && ++tick % 20 == 0) {
            double sum = 0; int priced = 0, items = 0;
            List<ItemStack> all = new ArrayList<>();
            for (int i = 0; i < 36; i++) all.add(mc.player.getInventory().getStack(i));
            all.add(mc.player.getOffHandStack());
            for (ItemStack s : all) {
                if (s.isEmpty()) continue;
                items++;
                Price p = price(Registries.ITEM.getId(s.getItem()).getPath());
                if (p.unit >= 0) { sum += p.unit * s.getCount(); priced++; }
            }
            invValue = sum; invPriced = priced; invItems = items;
        }
    }

    @Override public void onHud(DrawContext g, RenderTickCounter tc) {
        var tr = MinecraftClient.getInstance().textRenderer;
        List<String> lines = new ArrayList<>();
        if (DonutApi.token().isEmpty()) lines.add("Add API key: config/aether/donut_token.txt");
        else {
            if (showStats.get()) {
                lines.add("Money: " + (money < 0 ? "..." : DonutApi.fmt(money)));
                if (shards >= 0) lines.add("Shards: " + DonutApi.fmt(shards));
            }
            if (showInv.get()) lines.add("Inventory: " + DonutApi.fmt(invValue) + " (" + invPriced + "/" + invItems + " priced)");
            if (!DonutApi.lastError.isEmpty()) lines.add("API: " + DonutApi.lastError);
        }
        if (lines.isEmpty()) return;
        int w = 0; for (String l : lines) w = Math.max(w, tr.getWidth(l));
        int[] p = HudUtil.pos(corner.get(), offX.i(), offY.i(), w + 10, lines.size() * 10 + 4, g.getScaledWindowWidth(), g.getScaledWindowHeight());
        HudUtil.card(g, p[0], p[1], w + 10, lines.size() * 10 + 4, 0xFFFFC857);
        int y = p[1] + 3;
        for (String l : lines) { g.drawText(tr, l, p[0] + 6, y, 0xFFFFFFFF, true); y += 10; }
    }
}
