package com.reedwellarts.custompets.pet.goals;

import com.reedwellarts.custompets.pet.core.interfaces.OwnablePet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import org.jspecify.annotations.Nullable;

public class PetDefendOwnerGoal<T extends MobEntity & OwnablePet> extends ActiveTargetGoal<LivingEntity> {
    private @Nullable LivingEntity offender;
    private T pet;
    private int lastAttackedTime;

    public PetDefendOwnerGoal(T pet, final Class<LivingEntity> targetEntityClass, final boolean checkVisibility, final @Nullable boolean checkCanNavigate, final TargetPredicate.EntityPredicate targetPredicate) {
        super(pet, targetEntityClass, 10, checkVisibility, checkCanNavigate, targetPredicate);
        this.pet = pet;
    }

    public boolean canStart() {
        if (this.reciprocalChance > 0 && this.mob.getRandom().nextInt(this.reciprocalChance) != 0) {
            return false;
        } else {
            PlayerEntity owner = pet.getOwnerPlayer();
            if (owner != null) {
                this.offender = owner.getAttacker();
                int i = owner.getLastAttackedTime();
                return i != this.lastAttackedTime && this.canTrack(this.offender, this.targetPredicate);
            }
            return false;
        }
    }

    public void start() {
        this.setTargetEntity(this.offender);
        this.targetEntity = this.offender;
        if (pet.getOwnerPlayer() != null) {
            this.lastAttackedTime = pet.getOwnerPlayer().getLastAttackedTime();
        }

        if (pet instanceof FoxEntity) {
            pet.playSound(SoundEvents.ENTITY_FOX_AGGRO, 1.0F, 1.0F);
        }

        pet.setAttacking(true);

        super.start();
    }
}
