package dev.aether.mixin;

import dev.aether.module.impl.Freecam;
import dev.aether.module.impl.Freelook;
import dev.aether.module.impl.Hitboxes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getTargetingMargin()F", at = @At("RETURN"), cancellable = true)
    private void aether$hitbox(CallbackInfoReturnable<Float> cir) {
        Hitboxes h = Hitboxes.INSTANCE;
        Entity self = (Entity) (Object) this;
        if (h == null || !h.enabled || self == MinecraftClient.getInstance().player) return;
        if (h.applies(self)) cir.setReturnValue(cir.getReturnValue() + h.expand());
    }

    @Inject(method = "changeLookDirection(DD)V", at = @At("HEAD"), cancellable = true)
    private void aether$freelook(double dx, double dy, CallbackInfo ci) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        if (Freecam.active) {
            Freecam.yaw += (float) dx * 0.15f;
            Freecam.pitch = Math.max(-90f, Math.min(90f, Freecam.pitch + (float) dy * 0.15f));
            ci.cancel();
            return;
        }
        if (!Freelook.active) return;
        Freelook.yaw += (float) dx * 0.15f;
        Freelook.pitch = Math.max(-90f, Math.min(90f, Freelook.pitch + (float) dy * 0.15f));
        ci.cancel();
    }
}
