package com.quiettee.utils.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

@Mixin(ClientPacketListener.class)
public abstract class PlayerInfoConcurrencyMixin {
    @Shadow @Final @Mutable private Map<UUID, PlayerInfo> playerInfoMap;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void quiettee$concurrentPlayerInfo(CallbackInfo ci) {
        playerInfoMap = new ConcurrentHashMap<>(playerInfoMap);
    }
}
