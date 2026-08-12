package com.reedwellarts.custompets.event_handlers;

import com.reedwellarts.custompets.event_handlers.state.PetTrackingState;
import com.reedwellarts.custompets.pet.core.interfaces.OwnablePet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerDamageEventHandler {

    private static final Set<UUID> damageGuard = new HashSet<>();

    public static boolean handlePlayerDamage(LivingEntity livingEntity, DamageSource damageSource, float damageTaken) {
        if (!(livingEntity instanceof ServerPlayerEntity player)) return true;
        if (damageGuard.contains(player.getUuid())) return true;

        for (PetTrackingState.Entry entry : PetTrackingState.get(player.getEntityWorld()).getActivePets(player.getUuid())){
            UUID petUuid = entry.petUuid;
            Entity entity = player.getEntityWorld().getEntity(petUuid);

            if (entity instanceof OwnablePet pet && pet.getDelegate().getDataManager().getData().isGuardian){
                damageTaken = damageTaken / 2;

                damageGuard.add(player.getUuid());
                try {
                    pet.getDelegate().doDamage((MobEntity) pet, player.getEntityWorld(), damageSource, damageTaken);
                    player.damage(player.getEntityWorld(), damageSource, damageTaken);
                } finally {
                    damageGuard.remove(player.getUuid());
                }
                return false;
            }

            if (entity instanceof OwnablePet pet && entity instanceof LivingEntity petEntity && pet.getDelegate().getDataManager().getData().isAngel){
                if (damageTaken >= player.getHealth() && pet.getDelegate().doSacrifice()){
                    player.setHealth(player.getMaxHealth());
                    petEntity.setHealth(0);
                    petEntity.onDeath(player.getEntityWorld().getDamageSources().generic());
                    Text name = petEntity.hasCustomName() ? petEntity.getCustomName() : petEntity.getName();
                    player.sendMessage(Text.literal(name.getString() + " died to save you"));
                    return false;
                }
            }
        }
        return true;
    }
}
