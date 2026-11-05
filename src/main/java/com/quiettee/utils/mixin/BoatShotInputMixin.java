package com.quiettee.utils.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.quiettee.utils.modules.combat.BoatShot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class BoatShotInputMixin {

    @WrapOperation(method="handleKeybinds", at=@At(value="INVOKE", target="Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;releaseUsingItem(Lnet/minecraft/world/entity/player/Player;)V"))
    private void quiettee$keepAutomaticBow(MultiPlayerGameMode manager, Player player, Operation<Void> original) {
        BoatShot shot=BoatShot.active();
        if(shot==null || !shot.retainAutomaticDraw(player))original.call(manager,player);
    }
}
