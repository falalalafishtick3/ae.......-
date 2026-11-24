package dev.aether;

import dev.aether.config.ConfigManager;
import dev.aether.gui.ClickGuiScreen;
import dev.aether.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class AetherClient implements ClientModInitializer {
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("aether", "main"));

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        ConfigManager.load();
        dev.aether.AetherSounds.init();
        dev.aether.MenuBranding.init();
        dev.aether.Commands.init();
        dev.aether.module.impl.ChatAlert.init();
        dev.aether.module.impl.ChatTimestamps.init();
        dev.aether.module.impl.HudTweaks.init();
        ItemTooltipCallback.EVENT.register((stack, ctx, type, lines) -> dev.aether.module.impl.DonutEconomy.tooltip(stack, lines));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            dev.aether.module.impl.AutoReconnect.onJoin(client);
            if (!dev.aether.module.impl.ClickGuiModule.perServer()) return;
            String addr = client.getCurrentServerEntry() != null ? client.getCurrentServerEntry().address : "singleplayer";
            ConfigManager.useProfile(addr);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (dev.aether.module.impl.ClickGuiModule.perServer()) ConfigManager.useProfile(null);
        });
        HudElementRegistry.addLast(Identifier.of("aether", "hud"), (g, tc) -> ModuleManager.renderHud(g, tc));

        // VERSION-SENSITIVE: KeyBinding category type changed in 1.21.9+
        KeyBinding open = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aether.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (open.wasPressed()) mc.setScreen(new ClickGuiScreen());
            dev.aether.module.impl.AutoReconnect.screenTick(mc);
            if (mc.world != null && mc.player != null) ModuleManager.tick(mc);
        });

        // VERSION-SENSITIVE: Fabric reworked WorldRenderEvents in 1.21.9+
        WorldRenderEvents.END_MAIN.register(ctx -> ModuleManager.render(ctx));

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> ConfigManager.save());
    }
}
