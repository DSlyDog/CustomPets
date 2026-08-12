package com.reedwellarts.custompets.networking.payloads;

import com.reedwellarts.custompets.CustomPets;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record SendPetStatsSnapshotPayload(
        List<PetSnapshot> pets,
        int maxActive,
        boolean doRoster
) implements CustomPayload {

    public record PetSnapshot(
            String petUuid,
            String petType,
            String name,
            int health,
            int maxHealth,
            int level,
            int xp,
            int xpToNextLevel,
            List<String> unlockedSkills,
            List<String> activeSkills,
            boolean active
    ){
        public static final PacketCodec<RegistryByteBuf, PetSnapshot> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, PetSnapshot::petUuid,
                PacketCodecs.STRING, PetSnapshot::petType,
                PacketCodecs.STRING, PetSnapshot::name,
                PacketCodecs.INTEGER, PetSnapshot::health,
                PacketCodecs.INTEGER, PetSnapshot::maxHealth,
                PacketCodecs.INTEGER, PetSnapshot::level,
                PacketCodecs.INTEGER, PetSnapshot::xp,
                PacketCodecs.INTEGER, PetSnapshot::xpToNextLevel,
                PacketCodecs.collection(ArrayList::new, PacketCodecs.STRING), PetSnapshot::unlockedSkills,
                PacketCodecs.collection(ArrayList::new, PacketCodecs.STRING), PetSnapshot::activeSkills,
                PacketCodecs.BOOLEAN, PetSnapshot::active,
                PetSnapshot::new
        );
    }

    public static final Id<SendPetStatsSnapshotPayload> ID =
            new Id<>(Identifier.of(CustomPets.MOD_ID, "send_pet_stats"));

    public static final PacketCodec<RegistryByteBuf, SendPetStatsSnapshotPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.collection(ArrayList::new, PetSnapshot.CODEC), SendPetStatsSnapshotPayload::pets,
            PacketCodecs.INTEGER, SendPetStatsSnapshotPayload::maxActive,
            PacketCodecs.BOOLEAN, SendPetStatsSnapshotPayload::doRoster,
            SendPetStatsSnapshotPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
