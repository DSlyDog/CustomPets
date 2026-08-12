package com.reedwellarts.custompets.pet.core.interfaces;

import com.reedwellarts.custompets.pet.data.PetDelegate;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public interface OwnablePet {

    LivingEntity asLiving();
    PetDelegate getDelegate();

    boolean isPetSitting();
    void setPetSitting(boolean sitting);

    boolean isFlyable();
    void setFlyable(boolean flyable);

    boolean isFlightDescending();
    void setFlightDescending(boolean isDescending);

    EntityType getBaseType();

    void unsetPetRemoved();

    void applySavedVariant(@Nullable String variantId);

    default @Nullable PlayerEntity getOwnerPlayer() {
        LivingEntity self = asLiving();
        if (!(self.getEntityWorld() instanceof ServerWorld server)) return null;

        UUID uuid = getDelegate().getPetOwnerUuid();
        return uuid == null ? null : server.getPlayerByUuid(uuid);
    }
}
