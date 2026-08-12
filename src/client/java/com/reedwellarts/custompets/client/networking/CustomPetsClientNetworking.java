package com.reedwellarts.custompets.client.networking;

import com.reedwellarts.custompets.client.networking.snapshot.PetStatsSnapshot;
import com.reedwellarts.custompets.client.screen.PetNameScreen;
import com.reedwellarts.custompets.client.screen.PetRosterScreen;
import com.reedwellarts.custompets.client.screen.skilltree.PetSkillTreeScreen;
import com.reedwellarts.custompets.networking.payloads.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.List;

public class CustomPetsClientNetworking {

    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(OpenNameScreenPayload.ID, (payload, context) ->
                        context.client().execute(() ->
                                context.client().setScreen(
                                        new PetNameScreen(payload.petUuid())
                                )
                        )
                );

        ClientPlayNetworking.registerGlobalReceiver(SendPetStatsSnapshotPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    List<PetStatsSnapshot> entries = payload.pets().stream()
                                    .map(pet -> new PetStatsSnapshot(
                                            pet.petUuid(),
                                            pet.petType(),
                                            pet.name(),
                                            pet.health(),
                                            pet.maxHealth(),
                                            pet.level(),
                                            pet.xp(),
                                            pet.xpToNextLevel(),
                                            pet.unlockedSkills(),
                                            pet.activeSkills(),
                                            pet.active()
                                    ))
                                    .toList();

                    if (payload.doRoster()){
                        context.client().setScreen(
                                new PetRosterScreen(entries, payload.maxActive())
                        );
                    }else if (!entries.isEmpty()){
                        context.client().setScreen(
                                PetSkillTreeScreen.fromSnapshot(entries.getFirst())
                        );
                    }
                }));
    }

    public static void setEntityName(String name, String petUuid){
        ClientPlayNetworking.send(new SetEntityNamePayload(name, petUuid));
    }

    public static void setPetActiveSkills(String petUuid, List<String> activeSkills) {
        ClientPlayNetworking.send(new SetPetActiveSkillsPayload(petUuid, activeSkills));
    }

    public static void sendDismountNotice(String petUuid){
        ClientPlayNetworking.send(new PlayerDismountPayload(petUuid));
    }

    public static void setActivePets(List<String> petUuids){
        ClientPlayNetworking.send(new SetActivePetsPayload(petUuids));
    }
}
