package com.reedwellarts.custompets.event_handlers;

import com.reedwellarts.custompets.event_handlers.state.PetTrackingState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

public class RespawnEventHandler {

    public static void handlePetRespawn(MinecraftServer server){
        for (ServerWorld world : server.getWorlds()){
            PetTrackingState state = PetTrackingState.get(world);
            long now = world.getTime();

            List<PetTrackingState.Entry> due = state.getDueRespawns(now);
            for (PetTrackingState.Entry entry : due){
                ServerPlayerEntity owner = server.getPlayerManager().getPlayer(entry.ownerUuid);
                if (owner == null) continue;

                LivingEntity living = PetTrackingState.buildEntityFromEntry(entry, world, owner);
                if (living == null) continue;

                world.spawnEntity(living);
                state.completeRespawn(entry, living);
            }
        }
    }
}
