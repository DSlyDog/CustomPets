package com.reedwellarts.custompets.client;

import com.reedwellarts.custompets.CustomPets;
import com.reedwellarts.custompets.client.networking.CustomPetsClientNetworking;
import com.reedwellarts.custompets.pet.core.ModEntities;
import com.reedwellarts.custompets.pet.core.interfaces.OwnablePet;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.render.entity.EntityRendererFactories;
import net.minecraft.client.render.entity.FoxEntityRenderer;
import net.minecraft.client.render.entity.WolfEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomPetsClient implements ClientModInitializer {

    public static final String MOD_ID = "custompets";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        EntityRendererFactories.register(ModEntities.PET_FOX_ENTITY, FoxEntityRenderer::new);
        EntityRendererFactories.register(ModEntities.PET_WOLF_ENTITY, WolfEntityRenderer::new);

        CustomPetsClientNetworking.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.player.getVehicle() instanceof OwnablePet pet && pet.isFlyable()){
                if (client.options.sneakKey.isPressed()){
                    Entity petEntity = (Entity) pet;
                    BlockPos posBelow = petEntity.getBlockPos().down();
                    boolean isNearGround = petEntity.isOnGround() ||
                                           petEntity.getEntityWorld().getBlockState(posBelow).isSolidBlock(petEntity.getEntityWorld(), posBelow);
                    if (isNearGround && petEntity.getVelocity().y >= -0.1){
                        LOGGER.info("Client sees dismount");
                        client.player.dismountVehicle();
                        pet.setFlightDescending(false);
                        ((Entity) pet).removeAllPassengers();
                        CustomPetsClientNetworking.sendDismountNotice(((Entity) pet).getUuidAsString());
                    }else {
                        pet.setFlightDescending(true);
                    }
                }else{
                    pet.setFlightDescending(false);
                }
            }
        });
    }
}
