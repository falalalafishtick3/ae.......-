package dev.aether.module.impl;

import dev.aether.setting.*;
import net.minecraft.block.entity.*;

public class StorageFinder extends BlockEspModule {
    private final BoolSetting chests   = add(new BoolSetting("Chests", true));
    private final BoolSetting trapped  = add(new BoolSetting("Trapped Chests", true));
    private final BoolSetting ender    = add(new BoolSetting("Ender Chests", false));
    private final BoolSetting barrels  = add(new BoolSetting("Barrels", true));
    private final BoolSetting shulkers = add(new BoolSetting("Shulkers", true));
    private final BoolSetting droppers = add(new BoolSetting("Droppers", true));
    private final BoolSetting dispensers = add(new BoolSetting("Dispensers", false));
    private final BoolSetting hoppers  = add(new BoolSetting("Hoppers", false));

    private final ModeSetting colorScheme = add(new ModeSetting("Colors", "Per Type", "Single"));
    private final ColorSetting single   = add(new ColorSetting("Color", 0.50f, 0.9f, 1f));
    private final ColorSetting cChest   = add(new ColorSetting("Chest", 0.09f, 0.9f, 1f));
    private final ColorSetting cTrapped = add(new ColorSetting("Trapped", 0.00f, 0.9f, 1f));
    private final ColorSetting cEnder   = add(new ColorSetting("Ender", 0.78f, 0.8f, 1f));
    private final ColorSetting cBarrel  = add(new ColorSetting("Barrel", 0.06f, 0.7f, 0.8f));
    private final ColorSetting cShulker = add(new ColorSetting("Shulker", 0.83f, 0.6f, 1f));
    private final ColorSetting cDropper = add(new ColorSetting("Dropper", 0.58f, 0.7f, 1f));
    private final ColorSetting cDisp    = add(new ColorSetting("Dispenser", 0.40f, 0.7f, 1f));
    private final ColorSetting cHopper  = add(new ColorSetting("Hopper", 0.15f, 0.2f, 0.7f));

    public StorageFinder() {
        super("StorageFinder", "Highlights chests, shulkers, droppers and more");
        single.visibleWhen(() -> colorScheme.is("Single"));
        for (ColorSetting c : new ColorSetting[]{cChest, cTrapped, cEnder, cBarrel, cShulker, cDropper, cDisp, cHopper})
            c.visibleWhen(() -> colorScheme.is("Per Type"));
        addCommonSettings();
    }

    private int col(ColorSetting perType) { return colorScheme.is("Single") ? single.rgb() : perType.rgb(); }

    @Override protected Integer classify(BlockEntity be) {
        // Order matters: subclasses first.
        if (be instanceof TrappedChestBlockEntity) return trapped.get() ? col(cTrapped) : null;
        if (be instanceof ChestBlockEntity)        return chests.get()   ? col(cChest)   : null;
        if (be instanceof EnderChestBlockEntity)   return ender.get()    ? col(cEnder)   : null;
        if (be instanceof BarrelBlockEntity)       return barrels.get()  ? col(cBarrel)  : null;
        if (be instanceof ShulkerBoxBlockEntity)   return shulkers.get() ? col(cShulker) : null;
        if (be instanceof DropperBlockEntity)      return droppers.get() ? col(cDropper) : null;
        if (be instanceof DispenserBlockEntity)    return dispensers.get() ? col(cDisp)  : null;
        if (be instanceof HopperBlockEntity)       return hoppers.get()  ? col(cHopper)  : null;
        return null;
    }
}
