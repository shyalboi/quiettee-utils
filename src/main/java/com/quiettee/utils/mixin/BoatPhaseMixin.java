package com.quiettee.utils.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.quiettee.utils.modules.movement.BoatPhase;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractBoat.class)
public abstract class BoatPhaseMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
    private void quiettee$boatMove(AbstractBoat boat, MoverType type, Vec3 movement, Operation<Void> original) {
        BoatPhase module = BoatPhase.active();
        if (module == null) {
            original.call(boat, type, movement);
            return;
        }
        Vec3 step = module.movement(boat, movement);
        boolean oldNoClip = boat.noPhysics;
        try {
            if (module.phases(boat)) boat.noPhysics = true;
            original.call(boat, type, step);
        } finally {
            boat.noPhysics = oldNoClip;
        }
        module.afterMove(boat);
    }
}
