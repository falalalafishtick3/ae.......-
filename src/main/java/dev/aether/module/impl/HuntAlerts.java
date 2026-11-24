package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;

/** How you get told about finds: toast, ping sound, auto-copy coords, Discord webhook (config/aether/webhook.txt). */
public class HuntAlerts extends ClientModule {
    public static HuntAlerts INSTANCE;
    private final BoolSetting toast = add(new BoolSetting("Toast", true));
    private final BoolSetting sound = add(new BoolSetting("Ping Sound", true));
    private final BoolSetting copy = add(new BoolSetting("Copy Coords To Clipboard", false));
    private final BoolSetting screenshot = add(new BoolSetting("Screenshot", false));
    private final BoolSetting webhook = add(new BoolSetting("Discord Webhook", false));

    public HuntAlerts() { super("HuntAlerts", "Toast/sound/clipboard/Discord when something is found", Category.CLIENT); INSTANCE = this; enabled = true; }
    public boolean toast() { return toast.get(); }
    public boolean sound() { return sound.get(); }
    public boolean copy() { return copy.get(); }
    public boolean screenshot() { return screenshot.get(); }
    public boolean webhook() { return webhook.get(); }
}
