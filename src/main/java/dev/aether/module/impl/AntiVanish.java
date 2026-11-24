package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.Friends;
import dev.aether.util.LeaveUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Staff detector. Watches for players in SPECTATOR mode (tab list), spectator players sitting right next to / inside you
 * (what "spectating you" looks like), and invisible players near you. Can alert, panic (turn every cheat off) and/or disconnect.
 *
 * Limitation: if a vanish plugin hides the staff member from your client completely (no entity, no tab entry), nothing on the
 * client side can see them.
 */
public class AntiVanish extends ClientModule {
    private final BoolSetting tabSpectators = add(new BoolSetting("Spectators In Tab List", true));
    private final BoolSetting nearEntities  = add(new BoolSetting("Spectator Near You", true));
    private final NumberSetting nearRange   = add(new NumberSetting("Near Range (blocks)", 3.0, 1.0, 16.0, 0.5));
    private final BoolSetting invisibles    = add(new BoolSetting("Invisible Players", true));
    private final BoolSetting ignoreFriends = add(new BoolSetting("Ignore Friends", true));
    private final ModeSetting action        = add(new ModeSetting("On Detect", "Alert", "Panic", "Disconnect", "Panic + Disconnect"));
    private final BoolSetting sound         = add(new BoolSetting("Ping Sound", true));
    private final NumberSetting cooldown    = add(new NumberSetting("Repeat Alert After (s)", 20, 5, 120, 5));

    private final Map<String, Long> last = new HashMap<>();
    private int tick;

    public AntiVanish() { super("AntiVanish", "Detects staff spectating / vanished players", Category.CLIENT); }

    @Override public void onDisable() { last.clear(); }

    private static String profileName(Object profile) {
        try { return (String) profile.getClass().getMethod("name").invoke(profile); }
        catch (Exception e) {
            try { return (String) profile.getClass().getMethod("getName").invoke(profile); } catch (Exception e2) { return "unknown"; }
        }
    }

    private boolean due(String key) {
        long now = System.currentTimeMillis();
        Long t = last.get(key);
        if (t != null && now - t < cooldown.i() * 1000L) return false;
        last.put(key, now);
        return true;
    }

    private void detected(MinecraftClient mc, String level, String msg) {
        mc.player.sendMessage(Text.literal("[Aether] AntiVanish (" + level + "): " + msg), false);
        Notifications.pushText("AntiVanish: " + level, msg);
        if (sound.get()) mc.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.8f, 1.0f));
        String a = action.get();
        if (a.startsWith("Panic")) for (ClientModule m : ModuleManager.MODULES) if (m instanceof Panic) m.onEnable();
        if (a.endsWith("Disconnect")) { setEnabled(false); LeaveUtil.leave(mc, "AntiVanish", msg); }
    }

    @Override public void onTick(MinecraftClient mc) {
        if (++tick % 10 != 0) return;
        var p = mc.player;

        // 1) spectator players in the tab list
        if (tabSpectators.get() && mc.getNetworkHandler() != null) {
            for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
                if (e.getGameMode() != GameMode.SPECTATOR) continue;
                String name = profileName(e.getProfile());
                if (name.equalsIgnoreCase(p.getName().getString())) continue;
                if (ignoreFriends.get() && Friends.is(name)) continue;
                if (due("tab:" + name)) detected(mc, "spectator", name + " is in spectator mode");
            }
        }

        // 2) entities: spectators right next to you, invisible players
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof PlayerEntity o) || o == p) continue;
            String name = o.getName().getString();
            if (ignoreFriends.get() && Friends.is(name)) continue;
            double d = p.distanceTo(o);
            if (nearEntities.get() && o.isSpectator() && d <= nearRange.get()) {
                if (due("near:" + name)) detected(mc, "HIGH", name + " is spectating you (" + String.format("%.1f", d) + " blocks away)");
            } else if (invisibles.get() && o.isInvisible() && !o.isSpectator() && d <= 32) {
                if (due("inv:" + name)) detected(mc, "invisible", name + " is invisible near you (" + String.format("%.0f", d) + " blocks)");
            }
        }
    }
}
