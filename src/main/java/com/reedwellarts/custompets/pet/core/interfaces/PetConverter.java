package com.reedwellarts.custompets.pet.core.interfaces;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public interface PetConverter<S extends LivingEntity, P extends LivingEntity & OwnablePet> {
    EntityType<S> sourceType();
    EntityType<P> petType();

    default void copyCommon(S src, P pet, PlayerEntity owner){
        pet.refreshPositionAndAngles(src.getX(), src.getY(), src.getZ(), src.getYaw(), src.getPitch());
        pet.setBodyYaw(src.getBodyYaw());
        pet.setHeadYaw(src.getHeadYaw());
        pet.setHealth(Math.min(src.getHealth(), pet.getMaxHealth()));

        if (src.hasCustomName()){
            pet.setCustomName(src.getCustomName());
            pet.setCustomNameVisible(src.isCustomNameVisible());
        }

        pet.getDelegate().setPetOwnerUuid(owner.getUuid());
        pet.setPetSitting(false);
    }

    void copySpecific(S src, P pet);
}
