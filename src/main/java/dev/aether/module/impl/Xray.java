package dev.aether.module.impl;

import dev.aether.mixin.MinecraftClientAccessor;
import dev.aether.module.*;
import dev.aether.setting.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import org.lwjgl.glfw.GLFW;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Real X-ray: stops drawing every block except the ones you pick (face culling is overridden in BlockModelRendererMixin).
 * Not compatible with Sodium/Iris-style renderers. Default key: X.
 */
public class Xray extends ClientModule {
    public static volatile boolean active;
    private static volatile Set<Block> visible = Set.of();
    private static volatile boolean shulkers;

    public static boolean isVisible(BlockState s) {
        Block b = s.getBlock();
        return visible.contains(b) || (shulkers && b instanceof ShulkerBoxBlock);
    }

    private final Map<BoolSetting, Block[]> groups = new LinkedHashMap<>();
    private final BoolSetting fullbright = add(new BoolSetting("Fullbright", true));
    private final BoolSetting noCulling = add(new BoolSetting("Disable Cave Culling", true));

    private int lastSig = -1;
    private boolean nvApplied;

    private void group(String name, boolean def, Block... blocks) { groups.put(add(new BoolSetting(name, def)), blocks); }

    public Xray() {
        super("Xray", "See only the blocks you choose", Category.WORLD);
        key = GLFW.GLFW_KEY_X;
        group("Diamond", true, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        group("Emerald", true, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        group("Ancient Debris", true, Blocks.ANCIENT_DEBRIS);
        group("Gold", true, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
        group("Iron", true, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        group("Copper", false, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
        group("Redstone", true, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        group("Lapis", true, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        group("Coal", false, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
        group("Quartz", false, Blocks.NETHER_QUARTZ_ORE);
        group("Obsidian", false, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN);
        group("Spawners", true, Blocks.SPAWNER);
        group("Storage", true, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.BARREL);
    }

    private int signature() {
        int sig = 0, i = 0;
        for (BoolSetting b : groups.keySet()) { if (b.get()) sig |= 1 << i; i++; }
        return sig;
    }

    private void rebuild() {
        Set<Block> set = new HashSet<>();
        for (Map.Entry<BoolSetting, Block[]> e : groups.entrySet())
            if (e.getKey().get()) for (Block b : e.getValue()) set.add(b);
        visible = set;
        shulkers = groups.keySet().stream().anyMatch(b -> b.name.equals("Storage") && b.get());
        lastSig = signature();
    }

    private void reloadWorld(MinecraftClient mc) { if (mc.worldRenderer != null) mc.worldRenderer.reload(); }

    private void start(MinecraftClient mc) {
        rebuild();
        active = true;
        ((MinecraftClientAccessor) mc).aether$setChunkCulling(!noCulling.get());
        reloadWorld(mc);
    }

    @Override public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null) start(mc);
    }

    @Override public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        active = false;
        ((MinecraftClientAccessor) mc).aether$setChunkCulling(true);
        if (nvApplied && mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        nvApplied = false;
        if (mc.world != null) reloadWorld(mc);
    }

    @Override public void onTick(MinecraftClient mc) {
        if (!active) { start(mc); return; }
        if (signature() != lastSig) { rebuild(); reloadWorld(mc); }
        ((MinecraftClientAccessor) mc).aether$setChunkCulling(!noCulling.get());
        if (fullbright.get() && !mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, StatusEffectInstance.INFINITE, 0, false, false, false));
            nvApplied = true;
        } else if (!fullbright.get() && nvApplied) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            nvApplied = false;
        }
    }
}
