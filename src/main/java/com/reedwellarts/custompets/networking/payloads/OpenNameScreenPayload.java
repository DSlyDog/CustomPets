package com.reedwellarts.custompets.networking.payloads;

import com.reedwellarts.custompets.CustomPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenNameScreenPayload(String petUuid) implements CustomPayload {

    public static final Id<OpenNameScreenPayload> ID =
            new Id<>(Identifier.of(CustomPets.MOD_ID, "open_name_screen"));

    public static final PacketCodec<RegistryByteBuf, OpenNameScreenPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, OpenNameScreenPayload::petUuid,
                    OpenNameScreenPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId(){
        return ID;
    }
}
