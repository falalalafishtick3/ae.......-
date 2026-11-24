package dev.aether.mixin;

import dev.aether.module.impl.NewChunks;
import dev.aether.module.impl.PopCounter;
import dev.aether.module.impl.TpsHud;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import dev.aether.module.impl.SoundESP;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import dev.aether.module.impl.Velocity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onEntityVelocityUpdate(Lnet/minecraft/network/packet/s2c/play/EntityVelocityUpdateS2CPacket;)V",
            at = @At("HEAD"), cancellable = true)
    private void aether$velocity(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!mc.isOnThread()) return; // let vanilla re-queue onto the main thread first
        Velocity v = Velocity.INSTANCE;
        if (v == null || !v.enabled || mc.player == null || packet.getEntityId() != mc.player.getId()) return;
        ci.cancel();
        Vec3d vel = packet.getVelocity();
        mc.player.setVelocity(vel.x * v.h(), vel.y * v.v(), vel.z * v.h());
    }

    @Inject(method = "onEntityStatus(Lnet/minecraft/network/packet/s2c/play/EntityStatusS2CPacket;)V", at = @At("HEAD"))
    private void aether$pop(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PopCounter pc = PopCounter.INSTANCE;
        if (pc == null || !pc.enabled || !mc.isOnThread() || mc.world == null || mc.player == null || packet.getStatus() != 35) return;
        Entity e = packet.getEntity(mc.world);
        if (e instanceof PlayerEntity p) pc.onPop(mc, p.getName().getString(), p == mc.player);
    }

    @Inject(method = "onPlaySound(Lnet/minecraft/network/packet/s2c/play/PlaySoundS2CPacket;)V", at = @At("HEAD"))
    private void aether$sound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        SoundESP se = SoundESP.INSTANCE;
        if (se == null || !se.enabled || !mc.isOnThread() || mc.world == null || mc.player == null) return;
        String id = packet.getSound().getKey().map(k -> k.getValue().getPath()).orElse("");
        if (!id.isEmpty()) se.onSound(mc, id, packet.getX(), packet.getY(), packet.getZ());
    }

    @Inject(method = "onChunkData(Lnet/minecraft/network/packet/s2c/play/ChunkDataS2CPacket;)V", at = @At("TAIL"))
    private void aether$newChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        NewChunks nc = NewChunks.INSTANCE;
        if (nc == null || !nc.enabled || !mc.isOnThread() || mc.world == null) return;
        NewChunks.onChunk(mc, packet.getChunkX(), packet.getChunkZ());
    }

    @Inject(method = "onBlockUpdate(Lnet/minecraft/network/packet/s2c/play/BlockUpdateS2CPacket;)V", at = @At("HEAD"))
    private void aether$newChunkBlock(BlockUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        NewChunks nc = NewChunks.INSTANCE;
        if (nc == null || !nc.enabled || !mc.isOnThread() || mc.world == null) return;
        NewChunks.onFluidUpdate(mc, packet.getPos().getX(), packet.getPos().getZ(), packet.getState());
    }

    @Inject(method = "onChunkDeltaUpdate(Lnet/minecraft/network/packet/s2c/play/ChunkDeltaUpdateS2CPacket;)V", at = @At("HEAD"))
    private void aether$newChunkDelta(ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        NewChunks nc = NewChunks.INSTANCE;
        if (nc == null || !nc.enabled || !mc.isOnThread() || mc.world == null) return;
        packet.visitUpdates((pos, state) -> NewChunks.onFluidUpdate(mc, pos.getX(), pos.getZ(), state));
    }

    @Inject(method = "onWorldTimeUpdate(Lnet/minecraft/network/packet/s2c/play/WorldTimeUpdateS2CPacket;)V", at = @At("HEAD"))
    private void aether$tps(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
        TpsHud t = TpsHud.INSTANCE;
        if (t != null && t.enabled && MinecraftClient.getInstance().isOnThread()) t.onTimePacket();
    }
}
