package com.reedwellarts.custompets.pet.core;

import com.reedwellarts.custompets.CustomPets;
import com.reedwellarts.custompets.pet.entities.PetFoxEntity;
import com.reedwellarts.custompets.pet.entities.PetWolfEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final EntityType<PetFoxEntity> PET_FOX_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(CustomPets.MOD_ID, "pet_fox_entity"),
            EntityType.Builder.create(PetFoxEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 0.85f)
                    .build(RegistryKey.of(Registries.ENTITY_TYPE.getKey(), Identifier.of(CustomPets.MOD_ID, "pet_fox_entity")))
    );

    public static final EntityType<PetWolfEntity> PET_WOLF_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(CustomPets.MOD_ID, "pet_wolf_entity"),
            EntityType.Builder.create(PetWolfEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 0.85f)
                    .build(RegistryKey.of(Registries.ENTITY_TYPE.getKey(), Identifier.of(CustomPets.MOD_ID, "pet_wolf_entity")))
    );

    public static void registerModEntities() {}
}
