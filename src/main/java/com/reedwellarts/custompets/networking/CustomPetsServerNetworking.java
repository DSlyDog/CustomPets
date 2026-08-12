package com.reedwellarts.custompets.networking;

import com.reedwellarts.custompets.networking.payloads.*;
import com.reedwellarts.custompets.pet.skill.PetSkillState;
import com.reedwellarts.custompets.pet.data.PetData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

public class CustomPetsServerNetworking {

    public static void register(){
        PayloadTypeRegistry.playS2C().register(OpenNameScreenPayload.ID, OpenNameScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SendPetStatsSnapshotPayload.ID, SendPetStatsSnapshotPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(SetEntityNamePayload.ID, SetEntityNamePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SetPetActiveSkillsPayload.ID, SetPetActiveSkillsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PlayerDismountPayload.ID, PlayerDismountPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SetActivePetsPayload.ID, SetActivePetsPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SetEntityNamePayload.ID, (payload, context) ->
                CustomPetsServerCallback.namePet(payload.name(), payload.petUuid(), context));

        ServerPlayNetworking.registerGlobalReceiver(SetPetActiveSkillsPayload.ID, (payload, context) ->
                CustomPetsServerCallback.setPetActiveSkills(payload.petUuid(), payload.activeSkills(), context)
                );

        ServerPlayNetworking.registerGlobalReceiver(PlayerDismountPayload.ID, (payload, context) ->
                CustomPetsServerCallback.doDismountPlayer(payload.petUuid(), context)
                );

        ServerPlayNetworking.registerGlobalReceiver(SetActivePetsPayload.ID,
                CustomPetsServerCallback::updateActivePets);
    }

    public static void sendOpenScreen(ServerPlayerEntity player, String petUuid){
        ServerPlayNetworking.send(player, new OpenNameScreenPayload(petUuid));
    }

    public static void sendPetStatsSnapshot(ServerPlayerEntity player,
                                            List<SendPetStatsSnapshotPayload.PetSnapshot> pets,
                                            int maxActive,
                                            boolean doRoster){

        ServerPlayNetworking.send(player, new SendPetStatsSnapshotPayload(pets, maxActive, doRoster));
    }
}
