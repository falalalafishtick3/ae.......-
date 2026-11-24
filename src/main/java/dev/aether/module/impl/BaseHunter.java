package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.BaseScan;
import dev.aether.util.FoundBases;
import dev.aether.util.LeaveUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import java.util.*;

/**
 * Stash hunting loop for End bases: runs the RTP command, waits for chunks to load, scans for player-made
 * storage/bases (ignores End cities/ships), saves coordinates, then stays or disconnects. Built for DonutSMP-style /rtp end.
 */
public class BaseHunter extends EspModule {
    private enum S { SEND, WAIT, SETTLE, SCAN, DELAY, HOLD }
    private record Found(int cx, int cz, int score, int minY, int maxY) {}

    private final ModeSetting dimension   = add(new ModeSetting("Dimension", "End", "Nether", "Overworld"));
    private final BoolSetting startRtp    = add(new BoolSetting("RTP On Start", true));
    private final NumberSetting settle    = add(new NumberSetting("Settle Time (s)", 8, 2, 30, 1));
    private final NumberSetting scanTime  = add(new NumberSetting("Scan Time (s)", 12, 3, 60, 1));
    private final NumberSetting timeout   = add(new NumberSetting("RTP Retry After (s)", 30, 10, 120, 5));
    private final NumberSetting rtpDelay  = add(new NumberSetting("Delay Between RTPs (s)", 3, 0, 30, 1));
    private final NumberSetting maxRtps   = add(new NumberSetting("Max RTPs (0 = unlimited)", 0, 0, 500, 1));
    private final ModeSetting onFind      = add(new ModeSetting("On Find", "Disconnect", "Stay", "Keep Going"));
    private final BoolSetting leaveDone   = add(new BoolSetting("Disconnect When Done", true));
    private final NumberSetting minScore  = add(new NumberSetting("Min Base Score", 6, 1, 100, 1));
    private final NumberSetting minTypes  = add(new NumberSetting("Min Distinct Types", 2, 1, 8, 1));
    private final BoolSetting ignoreNatural = add(new BoolSetting("Ignore Natural Structures", true));
    private final BoolSetting leaveNearby = add(new BoolSetting("Leave If Player Nearby", true));
    private final NumberSetting nearbyRange = add(new NumberSetting("Nearby Range", 96, 16, 300, 4));
    private final BoolSetting leaveHurt   = add(new BoolSetting("Leave On Damage", true));
    private final BoolSetting save        = add(new BoolSetting("Save To Found Bases", true));
    private final ColorSetting color      = add(new ColorSetting("Color", 0.83f, 0.7f, 1f));

    private final Map<Long, Found> found = new LinkedHashMap<>();
    private S state = S.SEND;
    private int timer, rtpCount;
    private double sx, sz;
    private String dimAtSend = "";
    private boolean newFind, init;
    private float lastHealth = -1f;

    public BaseHunter() {
        super("BaseHunter", "Auto /rtp + scan for bases (End / Nether / Overworld), then stay or leave", Category.WORLD);
        nearbyRange.visibleWhen(leaveNearby::get);
        addCommonSettings();
        range.set(256.0);
        scanInterval.set(20.0);
    }

    private static String dim(MinecraftClient mc) { return mc.world.getRegistryKey().getValue().getPath(); }
    private String targetDim() { return dimension.is("End") ? "the_end" : dimension.is("Nether") ? "the_nether" : "overworld"; }
    private String rtpCommand() { return "rtp " + (dimension.is("End") ? "end" : dimension.is("Nether") ? "nether" : "overworld"); }
    private boolean inTarget(MinecraftClient mc) { return dim(mc).equals(targetDim()); }
    private void say(MinecraftClient mc, String msg) { mc.player.sendMessage(Text.literal("[Aether] BaseHunter: " + msg), false); }

    @Override public void onEnable() { init = false; rtpCount = 0; HuntStats.startMs = System.currentTimeMillis(); HuntStats.rtps = 0; HuntStats.finds = 0; }
    @Override public void onDisable() { super.onDisable(); found.clear(); init = false; HuntStats.status = "idle"; }

    // ---- detection (runs on the scan interval; also feeds the ESP boxes) ----
    @Override protected void scan(MinecraftClient mc, List<Target> out) {
        if (inTarget(mc) && (state == S.SETTLE || state == S.SCAN || state == S.HOLD)) detect(mc);
        for (Found f : found.values()) {
            double x0 = f.cx() * 16, z0 = f.cz() * 16;
            out.add(new Target(new Box(x0, f.minY() - 1, z0, x0 + 16, f.maxY() + 2, z0 + 16), color.rgb()));
        }
    }

    private void detect(MinecraftClient mc) {
        ChunkPos pc = mc.player.getChunkPos();
        int cr = Math.min(mc.options.getViewDistance().getValue() + 1, (int) Math.ceil(range.get() / 16.0));
        for (int dx = -cr; dx <= cr; dx++) for (int dz = -cr; dz <= cr; dz++) {
            int cx = pc.x + dx, cz = pc.z + dz;
            long key = new ChunkPos(cx, cz).toLong();
            if (found.containsKey(key)) continue;
            WorldChunk c = mc.world.getChunkManager().getWorldChunk(cx, cz);
            if (c == null) continue;
            BaseScan.Result r = BaseScan.scan(c, mc.world.getBottomY(), mc.world.getBottomY() + mc.world.getHeight(),
                    ignoreNatural.get() ? (dimension.is("End") ? BaseScan.END_STRUCTURE : dimension.is("Nether") ? BaseScan.NETHER_STRUCTURE : null) : null, minScore.i());
            if (r.score() < minScore.i() || r.types() < minTypes.i()) continue;
            if (ignoreNatural.get() && r.natural()) continue;
            found.put(key, new Found(cx, cz, r.score(), r.minY(), r.maxY()));
            newFind = true;
            String msg = "Possible base at " + (cx * 16) + ", " + (cz * 16) + " (score " + r.score() + ", y " + r.minY() + "-" + r.maxY() + ")";
            say(mc, msg);
            HuntStats.finds++;
            dev.aether.util.Alerts.fire(mc, "Base found", cx * 16 + 8, cz * 16 + 8, r.score());
            if (save.get()) saveToFile(mc, cx, cz, r);
        }
    }

    private void saveToFile(MinecraftClient mc, int cx, int cz, BaseScan.Result r) {
        FoundBases.Entry e = new FoundBases.Entry();
        e.server = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "singleplayer";
        e.dim = dim(mc); e.x = cx * 16 + 8; e.z = cz * 16 + 8; e.score = r.score(); e.minY = r.minY(); e.maxY = r.maxY();
        e.time = System.currentTimeMillis();
        FoundBases.add(e);
    }

    // ---- RTP loop ----
    private void finish(MinecraftClient mc, String why) {
        setEnabled(false);
        LeaveUtil.leave(mc, "BaseHunter", why);
    }

    @Override public void onTick(MinecraftClient mc) {
        super.onTick(mc); // detection + ESP
        var p = mc.player;

        if (!init) { init = true; state = (!startRtp.get() && inTarget(mc)) ? S.SETTLE : S.SEND; timer = settle.i() * 20; lastHealth = p.getHealth(); }

        if (leaveHurt.get() && p.getHealth() < lastHealth - 0.01f) { finish(mc, "took damage"); return; }
        lastHealth = p.getHealth();
        if (leaveNearby.get() && state != S.SEND) {
            for (Entity e : mc.world.getEntities())
                if (e instanceof PlayerEntity o && o != p && p.distanceTo(o) <= nearbyRange.get()) { finish(mc, "player nearby (" + o.getName().getString() + ")"); return; }
        }

        HuntStats.status = state.name().toLowerCase();
        switch (state) {
            case SEND -> {
                if (maxRtps.i() > 0 && rtpCount >= maxRtps.i()) {
                    if (leaveDone.get()) finish(mc, "finished " + rtpCount + " RTPs"); else { say(mc, "finished " + rtpCount + " RTPs"); state = S.HOLD; }
                    return;
                }
                if (mc.getNetworkHandler() == null) return;
                mc.getNetworkHandler().sendChatCommand(rtpCommand());
                rtpCount++;
                HuntStats.rtps++;
                sx = p.getX(); sz = p.getZ(); dimAtSend = dim(mc);
                timer = 0; state = S.WAIT;
                say(mc, "RTP #" + rtpCount);
            }
            case WAIT -> {
                timer++;
                boolean moved = Math.hypot(p.getX() - sx, p.getZ() - sz) > 64 || !dim(mc).equals(dimAtSend);
                if (moved && inTarget(mc)) { state = S.SETTLE; timer = settle.i() * 20; }
                else if (timer > timeout.i() * 20) state = S.SEND;
            }
            case SETTLE -> {
                if (newFind) { handleFind(mc); return; }
                if (--timer <= 0) { state = S.SCAN; timer = scanTime.i() * 20; }
            }
            case SCAN -> {
                if (newFind) { handleFind(mc); return; }
                if (--timer <= 0) { state = S.DELAY; timer = rtpDelay.i() * 20; }
            }
            case DELAY -> { if (--timer <= 0) state = S.SEND; }
            case HOLD -> {}
        }
    }

    private void handleFind(MinecraftClient mc) {
        newFind = false;
        switch (onFind.get()) {
            case "Disconnect" -> finish(mc, "base found");
            case "Stay" -> { state = S.HOLD; say(mc, "holding position (disable the module to continue)"); }
            default -> { state = S.DELAY; timer = rtpDelay.i() * 20; }
        }
    }
}
