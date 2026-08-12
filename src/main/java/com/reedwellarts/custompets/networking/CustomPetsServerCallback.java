package com.reedwellarts.custompets.networking;

import com.reedwellarts.custompets.event_handlers.state.PetTrackingState;
import com.reedwellarts.custompets.networking.payloads.SetActivePetsPayload;
import com.reedwellarts.custompets.pet.core.interfaces.OwnablePet;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.UUID;

public class CustomPetsServerCallback {

    public static void namePet(String name, String petUuid, ServerPlayNetworking.Context context){
        ServerWorld world = context.player().getEntityWorld();
        Entity entity = world.getEntity(UUID.fromString(petUuid));

        if (entity != null){
            entity.setCustomName(Text.literal(name));
            entity.setCustomNameVisible(true);
        }
    }

    public static void setPetActiveSkills(String petUuid, List<String> requestedActive, ServerPlayNetworking.Context context){
        ServerWorld world = context.player().getEntityWorld();
        Entity entity = world.getEntity(UUID.fromString(petUuid));
        if (!(entity instanceof OwnablePet ownable) || !(entity instanceof LivingEntity living)) return;

        var delegate = ownable.getDelegate();
        if (delegate.getPetOwnerUuid() == null || !delegate.getPetOwnerUuid().equals(context.player().getUuid())) return;

        var manager = delegate.getDataManager();
        var state = manager.getSkillState();

        for (Identifier id : state.getActive()){
            manager.deactivateSkill(id);
        }

        for (String raw : requestedActive){
            Identifier id = Identifier.tryParse(raw);
            if (id == null) continue;
            manager.activateSkill(id);
        }

        ownable.setFlyable(manager.isFlyable());
    }

    public static void doDismountPlayer(String petUuid, ServerPlayNetworking.Context context) {
        Entity entity = context.player().getEntityWorld().getEntity(UUID.fromString(petUuid));

        if (entity instanceof OwnablePet pet){
            pet.getDelegate().dismountPlayer((MobEntity) pet);
        }
    }

    public static void updateActivePets(SetActivePetsPayload payload, ServerPlayNetworking.Context context){
        PetTrackingState state = PetTrackingState.get(context.player().getEntityWorld());
        state.updateActivePets(context.player().getUuid(), payload.petUuids(), context.player().getEntityWorld());
    }
}
