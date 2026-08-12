package com.reedwellarts.custompets.event_handlers;

import com.reedwellarts.custompets.event_handlers.state.PetTrackingState;
import com.reedwellarts.custompets.pet.core.interfaces.OwnablePet;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class PlayerJoinEventHandler {

    public static void onJoin(ServerPlayerEntity player){
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return;

        PetTrackingState trackingState = PetTrackingState.get(world);

        for (PetTrackingState.Entry entry : trackingState.getActivePets(player.getUuid())){
            Entity liveEntity = world.getEntity(entry.petUuid);
            if (liveEntity instanceof OwnablePet pet){
                trackingState.registerLoadedPet(pet);
            }
        }
    }
}
