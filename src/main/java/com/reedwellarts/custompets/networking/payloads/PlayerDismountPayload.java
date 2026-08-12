package com.reedwellarts.custompets.networking.payloads;

import com.reedwellarts.custompets.CustomPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PlayerDismountPayload(String petUuid) implements CustomPayload {

    public static final Id<PlayerDismountPayload> ID =
            new Id<>(Identifier.of(CustomPets.MOD_ID, "player_dismount"));

    public static final PacketCodec<RegistryByteBuf, PlayerDismountPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, PlayerDismountPayload::petUuid,
                    PlayerDismountPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
