package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.util.HashMap;
import java.util.Map;

/** Counts totem pops (hook: ClientPlayNetworkHandlerMixin) and prints them in chat. */
public class PopCounter extends ClientModule {
    public static PopCounter INSTANCE;
    private final BoolSetting self = add(new BoolSetting("Include Yourself", true));
    private final Map<String, Integer> pops = new HashMap<>();

    public PopCounter() { super("PopCounter", "Chat message when someone pops a totem", Category.COMBAT); INSTANCE = this; }

    @Override public void onDisable() { pops.clear(); }

    public void onPop(MinecraftClient mc, String name, boolean isSelf) {
        if (isSelf && !self.get()) return;
        int n = pops.merge(name, 1, Integer::sum);
        mc.player.sendMessage(Text.literal("[Aether] " + (isSelf ? "You" : name) + " popped a totem (" + n + ")"), false);
    }
}
