package com.quiettee.utils.mixin;

import com.quiettee.utils.modules.movement.BoatPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class BoatPhaseNetworkMixin {
    @Inject(method = "handleMoveVehicle", at = @At("HEAD"))
    private void quiettee$boatCorrectionBefore(ClientboundMoveVehiclePacket packet, CallbackInfo info) {
        if (!Minecraft.getInstance().isSameThread()) return;
        BoatPhase module = BoatPhase.active();
        if (module != null) module.beforeVehicleCorrection((ClientPacketListener) (Object) this, packet);
    }

    @Inject(method = "handleMoveVehicle", at = @At("RETURN"))
    private void quiettee$boatCorrectionAfter(ClientboundMoveVehiclePacket packet, CallbackInfo info) {
        if (!Minecraft.getInstance().isSameThread()) return;
        BoatPhase module = BoatPhase.active();
        if (module != null) module.afterVehicleCorrection((ClientPacketListener) (Object) this, packet);
    }
}
