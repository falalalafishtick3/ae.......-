package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.LeaveUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

/** Reconnects to the last server after a disconnect (kick, restart...). Handy for AFK hunting. */
public class AutoReconnect extends ClientModule {
    public static AutoReconnect INSTANCE;
    private static ServerInfo lastServer;
    private static int attempts, countdown = -1;

    private final NumberSetting delay = add(new NumberSetting("Delay (s)", 5, 1, 120, 1));
    private final NumberSetting maxAttempts = add(new NumberSetting("Max Attempts", 5, 1, 50, 1));
    private final BoolSetting afterAether = add(new BoolSetting("Also After Aether Disconnects", false));

    public AutoReconnect() { super("AutoReconnect", "Rejoins the server after a disconnect", Category.CLIENT); INSTANCE = this; }

    public static void onJoin(MinecraftClient mc) {
        lastServer = mc.getCurrentServerEntry();
        attempts = 0; countdown = -1; LeaveUtil.aetherLeft = false;
    }

    /** Called every client tick (also on the disconnected screen). */
    public static void screenTick(MinecraftClient mc) {
        AutoReconnect m = INSTANCE;
        if (m == null || !m.enabled || lastServer == null || !(mc.currentScreen instanceof DisconnectedScreen)) { countdown = -1; return; }
        if (LeaveUtil.aetherLeft && !m.afterAether.get()) return;
        if (attempts >= m.maxAttempts.i()) return;
        if (countdown < 0) countdown = m.delay.i() * 20;
        if (--countdown > 0) return;
        countdown = -1; attempts++;
        ConnectScreen.connect(new TitleScreen(), mc, ServerAddress.parse(lastServer.address), lastServer, false, null);
    }
}
