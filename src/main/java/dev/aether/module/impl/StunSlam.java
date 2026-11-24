package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;
import dev.aether.util.InvUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

/** Axe hit to disable a raised shield, then mace smash. */
public class StunSlam extends ClientModule {
    private final BoolSetting axeFirst    = add(new BoolSetting("Axe Shield Break", true));
    private final BoolSetting needFall    = add(new BoolSetting("Require Falling", true));
    private final NumberSetting minFall   = add(new NumberSetting("Min Fall Distance", 1.5, 0, 10, 0.1));
    private final NumberSetting cooldownReq = add(new NumberSetting("Cooldown %", 90, 50, 100, 1));
    private final NumberSetting swapDelay = add(new NumberSetting("Swap Delay", 1, 0, 5, 1));
    private final BoolSetting onlyAttack  = add(new BoolSetting("Only While Attacking", true));
    private final BoolSetting switchBack  = add(new BoolSetting("Switch Back", true));

    private int stage = 0, timer = 0, prevSlot = -1, idle = 0;

    public StunSlam() { super("StunSlam", "Axe shield stun followed by a mace slam", Category.COMBAT); }

    @Override public void onDisable() { stage = 0; timer = 0; prevSlot = -1; }

    private void reset(MinecraftClient mc) {
        if (switchBack.get() && prevSlot >= 0 && mc.player != null) InvUtil.select(mc, prevSlot);
        stage = 0; prevSlot = -1; timer = 0;
    }

    @Override public void onTick(MinecraftClient mc) {
        if (mc.currentScreen != null) return;
        if (onlyAttack.get() && !mc.options.attackKey.isPressed()) { if (stage != 0) reset(mc); return; }
        ClientPlayerEntity p = mc.player;

        if (!(mc.crosshairTarget instanceof EntityHitResult ehr) || !(ehr.getEntity() instanceof LivingEntity target)) {
            if (stage != 0 && ++idle > 20) { reset(mc); idle = 0; }
            return;
        }
        idle = 0;
        if (prevSlot < 0) prevSlot = InvUtil.selected(mc);
        float ready = cooldownReq.f() / 100f;

        if (stage == 0) {
            if (axeFirst.get() && target.isBlocking()) {
                int axe = InvUtil.find(mc, s -> s.isIn(ItemTags.AXES));
                if (axe >= 0) {
                    InvUtil.select(mc, axe);
                    if (p.getAttackCooldownProgress(0f) >= ready) {
                        mc.interactionManager.attackEntity(p, target);
                        p.swingHand(Hand.MAIN_HAND);
                        stage = 1; timer = swapDelay.i();
                    }
                    return;
                }
            }
            stage = 1; timer = 0;
        }

        if (stage == 1) {
            if (timer-- > 0) return;
            int mace = InvUtil.find(mc, s -> s.isOf(Items.MACE));
            if (mace < 0) return;
            InvUtil.select(mc, mace);
            if (needFall.get() && p.fallDistance < minFall.get()) return;
            if (p.getAttackCooldownProgress(0f) >= ready) {
                mc.interactionManager.attackEntity(p, target);
                p.swingHand(Hand.MAIN_HAND);
                stage = 2; timer = 3;
            }
        } else if (stage == 2) {
            if (timer-- <= 0) reset(mc);
        }
    }
}
