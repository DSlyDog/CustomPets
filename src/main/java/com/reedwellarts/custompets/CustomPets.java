package com.reedwellarts.custompets;

import com.reedwellarts.custompets.event_handlers.*;
import com.reedwellarts.custompets.pet.core.ModEntities;
import com.reedwellarts.custompets.networking.CustomPetsServerNetworking;
import com.reedwellarts.custompets.items.ItemRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.WolfEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomPets implements ModInitializer {

    public static final String MOD_ID = "custompets";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModEntities.registerModEntities();
        ItemRegistry.registerModItems();
        CustomPetsServerNetworking.register();
        FabricDefaultAttributeRegistry.register(ModEntities.PET_FOX_ENTITY, FoxEntity.createFoxAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.PET_WOLF_ENTITY, WolfEntity.createWolfAttributes());

        UseEntityCallback.EVENT.register(TameEntityEventHandler::handleUseEntity);
        ServerTickEvents.END_SERVER_TICK.register(RespawnEventHandler::handlePetRespawn);
        ServerLivingEntityEvents.AFTER_DEATH.register(PlayerKillEventHandler::handleAfterDeath);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(PlayerDamageEventHandler::handlePlayerDamage);
        ServerPlayerEvents.JOIN.register(PlayerJoinEventHandler::onJoin);
    }
}
