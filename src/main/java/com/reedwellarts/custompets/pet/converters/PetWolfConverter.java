package com.reedwellarts.custompets.pet.converters;

import com.reedwellarts.custompets.pet.core.ModEntities;
import com.reedwellarts.custompets.pet.core.interfaces.PetConverter;
import com.reedwellarts.custompets.pet.entities.PetWolfEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.WolfEntity;

public class PetWolfConverter implements PetConverter<WolfEntity, PetWolfEntity> {
    @Override
    public EntityType<WolfEntity> sourceType() {
        return EntityType.WOLF;
    }

    @Override
    public EntityType<PetWolfEntity> petType() {
        return ModEntities.PET_WOLF_ENTITY;
    }

    @Override
    public void copySpecific(WolfEntity src, PetWolfEntity pet) {
        pet.setBreedingAge(src.getBreedingAge());
        pet.setTamed(true, true);
        pet.setOwner(pet.getDelegate().getPetOwnerAsLiving(pet));
    }
}
