package dev.aether.module.impl;

import com.google.gson.reflect.TypeToken;
import dev.aether.module.*;
import dev.aether.util.Store;
import net.minecraft.client.MinecraftClient;
import java.util.ArrayList;
import java.util.List;

/** Runs chat commands on a timer. Manage with: .cmd add <seconds> <command...> | list | remove <n> | clear */
public class AutoCommand extends ClientModule {
    public static class Entry { public int seconds = 60; public String command = ""; }
    private static List<Entry> list;
    private long[] next = new long[0];
    private static boolean dirty = true;

    public AutoCommand() { super("AutoCommand", "Sends commands on a timer (.cmd add <sec> <command>)", Category.WORLD); }

    private static synchronized List<Entry> ensure() {
        if (list == null) list = new ArrayList<>(Store.load("auto_commands.json", new TypeToken<List<Entry>>() {}.getType(), new ArrayList<Entry>()));
        return list;
    }
    public static synchronized List<Entry> all() { return new ArrayList<>(ensure()); }
    public static synchronized void add(int sec, String cmd) {
        Entry e = new Entry(); e.seconds = Math.max(5, sec); e.command = cmd.startsWith("/") ? cmd.substring(1) : cmd;
        ensure().add(e); Store.save("auto_commands.json", list); dirty = true;
    }
    public static synchronized boolean remove(int index) {
        if (index < 1 || index > ensure().size()) return false;
        ensure().remove(index - 1); Store.save("auto_commands.json", list); dirty = true; return true;
    }
    public static synchronized void clear() { ensure().clear(); Store.save("auto_commands.json", list); dirty = true; }

    @Override public void onEnable() { dirty = true; }

    @Override public void onTick(MinecraftClient mc) {
        List<Entry> l = all();
        long now = System.currentTimeMillis();
        if (dirty || next.length != l.size()) {
            next = new long[l.size()];
            for (int i = 0; i < next.length; i++) next[i] = now + l.get(i).seconds * 1000L;
            dirty = false;
        }
        if (mc.getNetworkHandler() == null) return;
        for (int i = 0; i < next.length; i++) {
            if (now < next[i]) continue;
            next[i] = now + l.get(i).seconds * 1000L;
            mc.getNetworkHandler().sendChatCommand(l.get(i).command);
        }
    }
}
