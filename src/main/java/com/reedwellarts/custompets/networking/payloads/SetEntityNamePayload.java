package com.reedwellarts.custompets.networking.payloads;

import com.reedwellarts.custompets.CustomPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SetEntityNamePayload(String name, String petUuid) implements CustomPayload {

    public static final Id<SetEntityNamePayload> ID =
            new Id<>(Identifier.of(CustomPets.MOD_ID, "set_entity_name"));

    public static final PacketCodec<RegistryByteBuf, SetEntityNamePayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, SetEntityNamePayload::name,
                    PacketCodecs.STRING, SetEntityNamePayload::petUuid,
                    SetEntityNamePayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId(){
        return ID;
    }
}
