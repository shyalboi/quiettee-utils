package com.quiettee.utils.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.quiettee.utils.events.PlayerPositionLookEvent;
import com.quiettee.utils.modules.combat.MaceSmash;
import com.quiettee.utils.modules.movement.FloatModule;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER))
    private void quiettee$onPlayerPositionLookHead(ClientboundPlayerPositionPacket packet, CallbackInfo ci,
          @Share("quietteeYaw") LocalFloatRef yawRef,
          @Share("quietteePitch") LocalFloatRef pitchRef,
          @Share("quietteeKeepLook") LocalBooleanRef keepLookRef,
          @Share("quietteeTrack") LocalBooleanRef trackRef,
          @Share("quietteePlayer") LocalRef<Entity> playerRef,
          @Share("quietteeWorld") LocalRef<ClientLevel> worldRef) {
        Minecraft client = Minecraft.getInstance();
        trackRef.set((Object) this == client.getConnection());
        if (!trackRef.get()) return;
        playerRef.set(client.player);
        worldRef.set(client.level);
        MeteorClient.EVENT_BUS.post(new PlayerPositionLookEvent.Before(packet));
        keepLookRef.set(client.player != null && (MaceSmash.keepLookArmed() || FloatModule.keepLookArmed()));
        if (!keepLookRef.get()) return;

        yawRef.set(client.player.getYRot());
        pitchRef.set(client.player.getXRot());
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void quiettee$onPlayerPositionLookReturn(ClientboundPlayerPositionPacket packet, CallbackInfo ci,
        @Share("quietteeYaw") LocalFloatRef yawRef,
        @Share("quietteePitch") LocalFloatRef pitchRef,
        @Share("quietteeKeepLook") LocalBooleanRef keepLookRef,
        @Share("quietteeTrack") LocalBooleanRef trackRef,
        @Share("quietteePlayer") LocalRef<Entity> playerRef,
        @Share("quietteeWorld") LocalRef<ClientLevel> worldRef) {
        if (!trackRef.get()) return;
        Minecraft client = Minecraft.getInstance();
        MaceSmash.consumeKeepLook();
        FloatModule.consumeKeepLook();
        if (keepLookRef.get() && client.player != null && client.player == playerRef.get()
            && client.level == worldRef.get() && (Object) this == client.getConnection()) {
            float savedYaw = yawRef.get();
            float savedPitch = pitchRef.get();

            client.player.setYRot(savedYaw + 0.000001f);
            client.player.setXRot(savedPitch + 0.000001f);
            client.player.yHeadRot = savedYaw;
            client.player.yBodyRot = savedYaw;
        }
        MeteorClient.EVENT_BUS.post(new PlayerPositionLookEvent.After(packet));
    }
}
