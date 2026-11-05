package com.quiettee.utils.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.quiettee.utils.modules.player.AirMiner;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerEntityMixin {
    @ModifyReturnValue(method = "getDestroySpeed", at = @At("RETURN"))
    private float quiettee$onGetBlockBreakingSpeed(float breakSpeed, BlockState block) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide()) return breakSpeed;

        AirMiner airMiner = Modules.get().get(AirMiner.class);
        if (airMiner != null && airMiner.liftsPenalty() && self == Minecraft.getInstance().player) breakSpeed *= 5.0f;

        return breakSpeed;
    }
}
