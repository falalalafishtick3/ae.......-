package dev.aether.module.impl;

import dev.aether.setting.ColorSetting;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.util.HashSet;
import java.util.Set;

public class SpawnerFinder extends BlockEspModule {
    private final ColorSetting color = add(new ColorSetting("Color", 0.95f, 0.85f, 1f));
    private final dev.aether.setting.BoolSetting chatTypes = add(new dev.aether.setting.BoolSetting("Chat Spawner Types", true));
    private final Set<Long> told = new HashSet<>();

    public SpawnerFinder() {
        super("SpawnerFinder", "Highlights mob spawners");
        addCommonSettings();
    }

    @Override public void onDisable() { super.onDisable(); told.clear(); }
    @Override protected Integer classify(BlockEntity be) {
        if (!(be instanceof MobSpawnerBlockEntity sp)) return null;
        if (chatTypes.get() && !told.contains(be.getPos().asLong())) {
            MinecraftClient mc = MinecraftClient.getInstance();
            try {
                var e = sp.getLogic().getRenderedEntity(mc.world, be.getPos());
                if (e != null) {
                    told.add(be.getPos().asLong());
                    mc.player.sendMessage(Text.literal("[Aether] Spawner (" + e.getType().getName().getString() + ") at "
                            + be.getPos().getX() + ", " + be.getPos().getY() + ", " + be.getPos().getZ()), false);
                }
            } catch (Throwable ignored) {}
        }
        return color.rgb();
    }
}
