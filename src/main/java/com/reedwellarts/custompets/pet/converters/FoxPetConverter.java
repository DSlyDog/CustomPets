package com.reedwellarts.custompets.pet.converters;

import com.reedwellarts.custompets.pet.core.ModEntities;
import com.reedwellarts.custompets.pet.core.interfaces.PetConverter;
import com.reedwellarts.custompets.pet.entities.PetFoxEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.FoxEntity;

public class FoxPetConverter implements PetConverter<FoxEntity, PetFoxEntity> {


    @Override
    public EntityType<FoxEntity> sourceType() {
        return EntityType.FOX;
    }

    @Override
    public EntityType<PetFoxEntity> petType() {
        return ModEntities.PET_FOX_ENTITY;
    }

    @Override
    public void copySpecific(FoxEntity src, PetFoxEntity pet) {
        pet.setBreedingAge(src.getBreedingAge());
        pet.applySavedVariant(src.getVariant().asString().toUpperCase());
    }
}
