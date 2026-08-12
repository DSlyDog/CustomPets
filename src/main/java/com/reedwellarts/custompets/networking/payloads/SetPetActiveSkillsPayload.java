package com.reedwellarts.custompets.networking.payloads;

import com.reedwellarts.custompets.CustomPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record SetPetActiveSkillsPayload(
        String petUuid,
        List<String> activeSkills
) implements CustomPayload {

    public static final Id<SetPetActiveSkillsPayload> ID =
            new Id<>(Identifier.of(CustomPets.MOD_ID, "set_pet_active_skills"));

    public static final PacketCodec<RegistryByteBuf, SetPetActiveSkillsPayload> CODEC =
            PacketCodec.tuple(
              PacketCodecs.STRING, SetPetActiveSkillsPayload::petUuid,
              PacketCodecs.collection(ArrayList::new, PacketCodecs.STRING), SetPetActiveSkillsPayload::activeSkills,
              SetPetActiveSkillsPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
