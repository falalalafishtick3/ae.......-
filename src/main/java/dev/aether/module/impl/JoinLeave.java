package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.Friends;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.HashSet;
import java.util.Set;

/** Tells you when players join or leave the server (tab list). */
public class JoinLeave extends ClientModule {
    private final BoolSetting joins = add(new BoolSetting("Joins", true));
    private final BoolSetting leaves = add(new BoolSetting("Leaves", true));
    private final BoolSetting friendsOnly = add(new BoolSetting("Friends Only", false));
    private final BoolSetting sound = add(new BoolSetting("Ping Sound", false));

    private Set<String> known;
    private Object worldRef;
    private int tick;

    public JoinLeave() { super("JoinLeave", "Alerts when players join / leave", Category.WORLD); }

    @Override public void onDisable() { known = null; }

    private static String profileName(Object profile) {
        try { return (String) profile.getClass().getMethod("name").invoke(profile); }
        catch (Exception e) {
            try { return (String) profile.getClass().getMethod("getName").invoke(profile); } catch (Exception e2) { return null; }
        }
    }

    @Override public void onTick(MinecraftClient mc) {
        if (++tick % 20 != 0 || mc.getNetworkHandler() == null) return;
        Set<String> now = new HashSet<>();
        for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
            String n = profileName(e.getProfile());
            if (n != null) now.add(n);
        }
        if (known == null || worldRef != mc.world) { known = now; worldRef = mc.world; return; }   // baseline after joining
        String me = mc.player.getName().getString();
        for (String n : now) if (!known.contains(n) && !n.equals(me)) notify(mc, n, true);
        for (String n : known) if (!now.contains(n) && !n.equals(me)) notify(mc, n, false);
        known = now;
    }

    private void notify(MinecraftClient mc, String name, boolean join) {
        if (join ? !joins.get() : !leaves.get()) return;
        if (friendsOnly.get() && !Friends.is(name)) return;
        mc.player.sendMessage(Text.literal("[Aether] " + name + (join ? " joined" : " left")).formatted(join ? Formatting.GREEN : Formatting.RED), false);
        if (sound.get()) mc.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), join ? 1.4f : 0.8f, 1.0f));
    }
}
