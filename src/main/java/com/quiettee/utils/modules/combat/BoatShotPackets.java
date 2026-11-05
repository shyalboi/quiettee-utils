package com.quiettee.utils.modules.combat;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;

public final class BoatShotPackets {
    public static boolean preserveQueuedRelease(Packet<?> packet,boolean automatic,boolean sameBow,int slot) {
        if(!sameBow)return false;
        return automatic && packet instanceof ServerboundUseItemPacket use && use.getHand()==InteractionHand.MAIN_HAND
            || packet instanceof ServerboundSetCarriedItemPacket sync && sync.getSlot()==slot;
    }
}
