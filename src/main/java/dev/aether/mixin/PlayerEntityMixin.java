package dev.aether.mixin;

import dev.aether.module.impl.Reach;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "getEntityInteractionRange()D", at = @At("RETURN"), cancellable = true)
    private void aether$entityReach(CallbackInfoReturnable<Double> cir) {
        Reach r = Reach.INSTANCE;
        if (r != null && r.enabled && (Object) this == MinecraftClient.getInstance().player)
            cir.setReturnValue(Math.max(cir.getReturnValue(), r.entityRange()));
    }

    @Inject(method = "getBlockInteractionRange()D", at = @At("RETURN"), cancellable = true)
    private void aether$blockReach(CallbackInfoReturnable<Double> cir) {
        Reach r = Reach.INSTANCE;
        if (r != null && r.enabled && (Object) this == MinecraftClient.getInstance().player)
            cir.setReturnValue(Math.max(cir.getReturnValue(), r.blockRange()));
    }
}
