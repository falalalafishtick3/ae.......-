package dev.aether.mixin;

import dev.aether.module.impl.Criticals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class AttackMixin {
    @Inject(method = "attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"))
    private void aether$crit(PlayerEntity player, Entity target, CallbackInfo ci) {
        Criticals c = Criticals.INSTANCE;
        if (c != null && c.enabled) c.apply(MinecraftClient.getInstance());
    }
}
