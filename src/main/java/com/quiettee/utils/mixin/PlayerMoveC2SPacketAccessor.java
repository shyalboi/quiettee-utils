package com.quiettee.utils.mixin;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMovePlayerPacket.class)
public interface PlayerMoveC2SPacketAccessor {
    @Mutable
    @Accessor("x")
    void quiettee$setX(double x);

    @Mutable
    @Accessor("y")
    void quiettee$setY(double y);

    @Mutable
    @Accessor("z")
    void quiettee$setZ(double z);

    @Mutable
    @Accessor("yRot")
    void quiettee$setYaw(float yaw);

    @Mutable
    @Accessor("xRot")
    void quiettee$setPitch(float pitch);

    @Mutable
    @Accessor("onGround")
    void quiettee$setOnGround(boolean onGround);

    @Mutable
    @Accessor("horizontalCollision")
    void quiettee$setHorizontalCollision(boolean horizontalCollision);
}
