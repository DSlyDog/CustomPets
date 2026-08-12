package com.reedwellarts.custompets.event_handlers;

import com.reedwellarts.custompets.event_handlers.state.PetTrackingState;
import com.reedwellarts.custompets.pet.core.interfaces.OwnablePet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;

public class PlayerKillEventHandler {

    public static void handleAfterDeath(Entity entity, DamageSource source){
        if (source.getAttacker() instanceof ServerPlayerEntity player){
            List<PetTrackingState.Entry> ownablePets = PetTrackingState.get(player.getEntityWorld()).getActivePets(player.getUuid());
            for (PetTrackingState.Entry entry : ownablePets){
                UUID petUuid = entry.petUuid;
                Entity petEntity = player.getEntityWorld().getEntity(petUuid);
                if (petEntity instanceof OwnablePet pet) {
                    boolean petHit = pet.getDelegate().onPlayerKilledTarget(entity);
                }
            }
        }
    }
}
