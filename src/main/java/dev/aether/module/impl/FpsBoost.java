package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.particle.ParticlesMode;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * One-click FPS boost: lowers the vanilla graphics options that cost the most, remembers your original values and puts them
 * back when you turn the module off. (For a much bigger gain install Sodium + Lithium, this module is a quick lever, not a replacement.)
 */
public class FpsBoost extends ClientModule {
    private final BoolSetting particles   = add(new BoolSetting("Minimal Particles", true));
    private final BoolSetting clouds      = add(new BoolSetting("Clouds Off", true));
    private final BoolSetting shadows     = add(new BoolSetting("Entity Shadows Off", true));
    private final BoolSetting smooth      = add(new BoolSetting("Smooth Lighting Off", false));
    private final BoolSetting bobbing     = add(new BoolSetting("View Bobbing Off", false));
    private final BoolSetting blend       = add(new BoolSetting("Biome Blend = 1", true));
    private final BoolSetting unlock      = add(new BoolSetting("Unlock FPS + VSync Off", true));
    private final BoolSetting limitView   = add(new BoolSetting("Limit Render Distance", true));
    private final NumberSetting maxView   = add(new NumberSetting("Max Render Distance", 10, 2, 32, 1));
    private final BoolSetting limitSim    = add(new BoolSetting("Limit Simulation Distance", true));
    private final NumberSetting maxSim    = add(new NumberSetting("Max Simulation Distance", 8, 5, 32, 1));
    private final BoolSetting limitEnt    = add(new BoolSetting("Limit Entity Distance", false));
    private final NumberSetting maxEnt    = add(new NumberSetting("Entity Distance Scale", 0.75, 0.5, 5.0, 0.05));

    private final Map<SimpleOption<?>, Object> saved = new IdentityHashMap<>();
    private boolean applied;

    public FpsBoost() {
        super("FpsBoost", "Lowers costly graphics options (restores them when off)", Category.RENDER);
        maxView.visibleWhen(limitView::get);
        maxSim.visibleWhen(limitSim::get);
        maxEnt.visibleWhen(limitEnt::get);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void put(SimpleOption opt, Object value) {
        try {
            saved.putIfAbsent(opt, opt.getValue());
            opt.setValue(value);
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void capInt(SimpleOption opt, int cap) {
        try {
            int cur = (Integer) opt.getValue();
            if (cur > cap) put(opt, cap);
        } catch (Throwable ignored) {}
    }

    private void apply(MinecraftClient mc) {
        var o = mc.options;
        if (particles.get()) put(o.getParticles(), ParticlesMode.MINIMAL);
        if (clouds.get()) put(o.getCloudRenderMode(), CloudRenderMode.OFF);
        if (shadows.get()) put(o.getEntityShadows(), Boolean.FALSE);
        if (smooth.get()) put(o.getAo(), Boolean.FALSE);
        if (bobbing.get()) put(o.getBobView(), Boolean.FALSE);
        if (blend.get()) put(o.getBiomeBlendRadius(), 1);
        if (unlock.get()) { put(o.getMaxFps(), 260); put(o.getEnableVsync(), Boolean.FALSE); }
        if (limitView.get()) capInt(o.getViewDistance(), maxView.i());
        if (limitSim.get()) capInt(o.getSimulationDistance(), maxSim.i());
        if (limitEnt.get()) put(o.getEntityDistanceScaling(), (double) maxEnt.f());
        applied = true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void restore() {
        for (Map.Entry<SimpleOption<?>, Object> e : saved.entrySet()) {
            try { ((SimpleOption) e.getKey()).setValue(e.getValue()); } catch (Throwable ignored) {}
        }
        saved.clear();
        applied = false;
    }

    @Override public void onEnable() { MinecraftClient mc = MinecraftClient.getInstance(); if (mc.options != null) apply(mc); }
    @Override public void onDisable() { restore(); }

    @Override public void onTick(MinecraftClient mc) {
        if (!applied) apply(mc);   // enabled from a saved config at startup
    }
}
