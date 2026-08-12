package com.reedwellarts.custompets.networking.payloads;

import com.reedwellarts.custompets.CustomPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record SetActivePetsPayload(
        List<String> petUuids
) implements CustomPayload {

    public static final Id<SetActivePetsPayload> ID =
            new Id<>(Identifier.of(CustomPets.MOD_ID, "set_active_pets"));

    public static final PacketCodec<RegistryByteBuf, SetActivePetsPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.collection(ArrayList::new, PacketCodecs.STRING), SetActivePetsPayload::petUuids,
                    SetActivePetsPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
