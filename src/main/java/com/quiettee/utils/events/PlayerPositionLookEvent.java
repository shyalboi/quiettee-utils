package com.quiettee.utils.events;

import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;

public abstract class PlayerPositionLookEvent {
    public final ClientboundPlayerPositionPacket packet;

    private PlayerPositionLookEvent(ClientboundPlayerPositionPacket packet) {
        this.packet = packet;
    }

    public static final class Before extends PlayerPositionLookEvent {
        public Before(ClientboundPlayerPositionPacket packet) {
            super(packet);
        }
    }

    public static final class After extends PlayerPositionLookEvent {
        public After(ClientboundPlayerPositionPacket packet) {
            super(packet);
        }
    }
}
