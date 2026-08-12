package com.reedwellarts.custompets.event_handlers;

import com.reedwellarts.custompets.event_handlers.state.PetTrackingState;
import com.reedwellarts.custompets.items.ItemRegistry;
import com.reedwellarts.custompets.pet.converters.FoxPetConverter;
import com.reedwellarts.custompets.pet.converters.PetWolfConverter;
import com.reedwellarts.custompets.pet.core.interfaces.OwnablePet;
import com.reedwellarts.custompets.pet.core.interfaces.PetConverter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.Map;

public class TameEntityEventHandler {

    private static final Map<EntityType<?>, PetConverter<?, ?>> CONVERTERS = Map.of(
            EntityType.FOX, new FoxPetConverter(),
            EntityType.WOLF, new PetWolfConverter()
            //EntityType.ENDER_DRAGON, new DragonPetConverter()
    );

    public static ActionResult handleUseEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (world.isClient()) return ActionResult.PASS;
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (!player.getMainHandStack().isOf(ItemRegistry.PET_WAND)) return ActionResult.PASS;

        if (!(entity instanceof LivingEntity living)) return ActionResult.PASS;

        PetConverter<?, ?> raw = CONVERTERS.get(living.getType());
        if (raw == null) return ActionResult.PASS;

        return convert(player, world, living, raw);
    }

    @SuppressWarnings("unchecked")
    private static <S extends LivingEntity, P extends LivingEntity & OwnablePet>
    ActionResult convert(PlayerEntity player, World world, LivingEntity srcAny, PetConverter<?, ?> raw){
        PetConverter<S, P> converter = (PetConverter<S, P>) raw;
        S src = (S) srcAny;

        P pet = converter.petType().create(world, SpawnReason.CONVERSION);
        if (pet == null) return ActionResult.PASS;

        converter.copyCommon(src, pet, player);
        converter.copySpecific(src, pet);

        PetTrackingState trackingState = PetTrackingState.get((ServerWorld) world);

        boolean success = trackingState.tamePet(pet, player.getUuid());
        if (!success) {
            return ActionResult.PASS;
        }

        world.spawnEntity(pet);
        src.discard();
        return ActionResult.SUCCESS;
    }
}
