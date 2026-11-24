package dev.aether.mixin;

import dev.aether.module.impl.Xray;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** X-ray: while active, only the chosen blocks get faces drawn; everything else is skipped. */
@Mixin(BlockModelRenderer.class)
public abstract class BlockModelRendererMixin {
    @Inject(method = "shouldDrawFace(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;ZLnet/minecraft/util/math/Direction;Lnet/minecraft/util/math/BlockPos;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void aether$xray(BlockRenderView world, BlockState state, boolean cull, Direction side, BlockPos pos,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (Xray.active) cir.setReturnValue(Xray.isVisible(state));
    }
}
